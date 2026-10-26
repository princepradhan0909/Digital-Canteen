
package com.giit.campusbite;

import com.giit.campusbite.dto.OrderStatusHistoryDTO;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final BigDecimal MAX_SNAPSHOT_PRICE =
            new BigDecimal("99999999.99");
    private static final BigDecimal MAX_ORDER_TOTAL =
            new BigDecimal("9999999999.99");

    private final CustomerOrderRepository orderRepository;
    private final FoodItemRepository foodRepository;
    private final FoodVariantRepository variantRepository;
    private final FoodAddonRepository addonRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final NotificationService notificationService;
    private final RazorpayService razorpayService;
    private final StudentUserRepository users;

    public OrderService(
            CustomerOrderRepository orderRepository,
            FoodItemRepository foodRepository,
            FoodVariantRepository variantRepository,
            FoodAddonRepository addonRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            NotificationService notificationService,
            RazorpayService razorpayService,
            StudentUserRepository users) {

        this.orderRepository = orderRepository;
        this.foodRepository = foodRepository;
        this.variantRepository = variantRepository;
        this.addonRepository = addonRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.notificationService = notificationService;
        this.razorpayService = razorpayService;
        this.users = users;
    }

    // Create a new order
    @Transactional
    public CustomerOrder createOrder(OrderRequest request, String studentEmail) {

        if (request == null || request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one food item");
        }

        if (request.getStudentName() == null
                || request.getStudentName().isBlank()) {
            throw new IllegalArgumentException(
                    "Student name is required");
        }

        if (request.getStudentPhone() == null
                || request.getStudentPhone().isBlank()) {
            throw new IllegalArgumentException(
                    "Student phone is required");
        }

        if (request.getPaymentMethod() == null
                || request.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException(
                    "Payment method is required");
        }

        String paymentMethod = request.getPaymentMethod()
                .trim().toUpperCase();

        if ("ONLINE".equals(paymentMethod)) paymentMethod = "RAZORPAY";
        if (!Set.of("COUNTER", "RAZORPAY").contains(paymentMethod)) {
            throw new IllegalArgumentException(
                    "Payment method must be COUNTER or RAZORPAY");
        }
        if ("RAZORPAY".equals(paymentMethod)) {
            razorpayService.requireConfigured();
        }

        CustomerOrder order = new CustomerOrder(
                request.getStudentName().trim(),
                request.getStudentPhone().trim(),
                0.0
        );

        if (studentEmail != null) {
            StudentUser student = users.findByEmailIgnoreCase(studentEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Student account not found"));
            order.setStudentName(student.getName());
            order.setStudentPhone(student.getPhone());
        }
        order.setOrderStatus("PLACED");
        order.setStudentEmail(studentEmail);
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderLineRequest line : request.getItems()) {

            if (line == null || line.getFoodItemId() == null) {
                throw new IllegalArgumentException(
                        "Food item ID is required");
            }

            if (line.getQuantity() == null || line.getQuantity() < 1) {
                throw new IllegalArgumentException(
                        "Quantity must be at least 1");
            }

            FoodItem food = foodRepository.findById(line.getFoodItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Food item not found: "
                                    + line.getFoodItemId()));

            if (!food.isAvailable()) {
                throw new IllegalArgumentException(
                        food.getName() + " is currently unavailable");
            }

            int quantity = line.getQuantity();
            LinePricing pricing = resolveLinePricing(food, line);
            total = total.add(pricing.unitPrice()
                    .multiply(BigDecimal.valueOf(quantity)));
            if (total.compareTo(MAX_ORDER_TOTAL) > 0) {
                throw new IllegalArgumentException(
                        "Order total exceeds the supported payment limit");
            }

            OrderItem orderItem = new OrderItem(
                    order, food, quantity, pricing.unitPrice().doubleValue());
            orderItem.setUnitPriceAmount(pricing.unitPrice());
            orderItem.setBasePriceAmount(pricing.basePrice());
            if (pricing.variant() != null) {
                orderItem.setVariantId(pricing.variant().getId());
                orderItem.setVariantName(pricing.variant().getName());
                orderItem.setVariantPriceAmount(pricing.variant().getPrice());
            }
            orderItem.setAddons(pricing.addons());
            orderItems.add(orderItem);
        }

        order.setTotalAmountDecimal(total);
        order = orderRepository.save(order);

        orderItemRepository.saveAll(orderItems);

        Payment payment = new Payment(order, paymentMethod);
        payment.setPaymentStatus("PENDING");
        paymentRepository.save(payment);

        // Save initial order status in history
        statusHistoryRepository.save(
                new OrderStatusHistory(order, "PLACED")
        );
        notificationService.notifyOrder(order,
                "Your order #" + order.getId() + " was received.");

        return order;
    }

    private LinePricing resolveLinePricing(
            FoodItem food, OrderRequest.OrderLineRequest line) {
        BigDecimal basePrice = food.getPriceAmount();
        if (basePrice == null) {
            throw new IllegalArgumentException(
                    food.getName() + " does not have a valid price");
        }
        if (basePrice.compareTo(MAX_SNAPSHOT_PRICE) > 0) {
            throw new IllegalArgumentException(
                    food.getName() + " exceeds the supported item price limit");
        }

        List<FoodVariant> availableVariants = variantRepository
                .findAllByFoodItem_IdAndAvailableTrueOrderByDisplayOrderAscNameAsc(
                        food.getId());
        FoodVariant selectedVariant = null;
        if (line.getVariantId() != null) {
            selectedVariant = variantRepository.findById(line.getVariantId())
                    .filter(variant -> variant.isAvailable()
                            && food.getId().equals(variant.getFoodItem().getId()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Selected variant is unavailable for " + food.getName()));
        } else if (!availableVariants.isEmpty()) {
            throw new IllegalArgumentException(
                    "Choose a variant for " + food.getName());
        }

        List<Long> addonIds = line.getAddonIds() == null
                ? List.of() : line.getAddonIds();
        if (addonIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Add-on IDs cannot be null");
        }
        if (new HashSet<>(addonIds).size() != addonIds.size()) {
            throw new IllegalArgumentException(
                    "The same add-on cannot be selected more than once");
        }

        Map<Long, FoodAddon> availableAddons = addonRepository
                .findAllById(addonIds).stream()
                .collect(Collectors.toMap(FoodAddon::getId, addon -> addon));
        if (availableAddons.size() != addonIds.size()) {
            throw new IllegalArgumentException(
                    "One or more selected add-ons are unavailable");
        }

        List<OrderItem.AddonSnapshot> addonSnapshots = new ArrayList<>();
        BigDecimal unitPrice = selectedVariant == null
                ? basePrice : selectedVariant.getPrice();
        for (Long addonId : addonIds) {
            FoodAddon addon = availableAddons.get(addonId);
            if (!addon.isAvailable()
                    || !food.getId().equals(addon.getFoodItem().getId())) {
                throw new IllegalArgumentException(
                        "Selected add-on is unavailable for " + food.getName());
            }
            unitPrice = unitPrice.add(addon.getPrice());
            addonSnapshots.add(new OrderItem.AddonSnapshot(
                    addon.getId(), addon.getName(), addon.getPrice()));
        }
        if (unitPrice.compareTo(MAX_SNAPSHOT_PRICE) > 0) {
            throw new IllegalArgumentException(
                    food.getName() + " exceeds the supported item price limit");
        }
        return new LinePricing(
                basePrice, selectedVariant, addonSnapshots, unitPrice);
    }

    private record LinePricing(
            BigDecimal basePrice,
            FoodVariant variant,
            List<OrderItem.AddonSnapshot> addons,
            BigDecimal unitPrice) {
    }

    @Transactional
    public CustomerOrder createOrder(OrderRequest request) {
        return createOrder(request, null);
    }

    // Get an order by ID
    public Optional<CustomerOrder> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    // Update order status with transition validation
    @Transactional
    public CustomerOrder updateOrderStatus(
            Long orderId, String newStatus) {

        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found"));

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        String status = newStatus.trim().toUpperCase();
        String currentStatus = order.getOrderStatus();

        if (!Set.of(
                "PLACED", "PREPARING", "READY",
                "COMPLETED", "CANCELLED"
        ).contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid order status: " + status);
        }

        if (status.equals(currentStatus)) {
            return order;
        }

        boolean allowed = switch (currentStatus) {
            case "PLACED" ->
                    status.equals("PREPARING")
                    || status.equals("CANCELLED");

            case "PREPARING" ->
                    status.equals("READY")
                    || status.equals("CANCELLED");

            case "READY" ->
                    status.equals("COMPLETED");

            case "COMPLETED", "CANCELLED" -> false;

            default -> false;
        };

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Cannot change order status from "
                            + currentStatus + " to " + status);
        }

        order.setOrderStatus(status);
        CustomerOrder savedOrder = orderRepository.save(order);

        statusHistoryRepository.save(
                new OrderStatusHistory(savedOrder, status)
        );
        notificationService.notifyOrder(savedOrder,
                "Order #" + savedOrder.getId() + " is now " +
                        status.toLowerCase() + ".");

        return savedOrder;
    }

    public List<CustomerOrder> getOrdersForStudent(String email) {
        return orderRepository.findByStudentEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    public boolean canStudentAccessOrder(Long orderId, String email) {
        return orderRepository.findByIdAndStudentEmailIgnoreCase(orderId, email)
                .isPresent();
    }

    @Transactional
    public Map<String, Object> createRazorpayOrder(Long orderId, String email) {
        CustomerOrder order = findOwnedOrder(orderId, email);
        Payment payment = paymentRepository.findByCustomerOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (!"RAZORPAY".equals(payment.getPaymentMethod())) {
            throw new IllegalArgumentException("This order uses counter payment");
        }
        Map<String, Object> gatewayOrder = razorpayService.createOrder(payment);
        paymentRepository.save(payment);
        return gatewayOrder;
    }

    @Transactional
    public Payment verifyRazorpayPayment(Long orderId, String email,
                                         String gatewayOrderId,
                                         String gatewayPaymentId,
                                         String signature) {
        findOwnedOrder(orderId, email);
        Payment payment = paymentRepository.findByCustomerOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        razorpayService.verify(payment, gatewayOrderId, gatewayPaymentId, signature);
        Payment saved = paymentRepository.save(payment);
        notificationService.notifyOrder(saved.getCustomerOrder(),
                "Payment for order #" + orderId + " was successful.");
        return saved;
    }

    private CustomerOrder findOwnedOrder(Long orderId, String email) {
        return orderRepository.findByIdAndStudentEmailIgnoreCase(orderId, email)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    // Get all items belonging to an order
    public List<OrderItem> getOrderItems(Long orderId) {

        if (!orderRepository.existsById(orderId)) {
            throw new IllegalArgumentException("Order not found");
        }

        return orderItemRepository.findByCustomerOrderId(orderId);
    }

    // Get payment details
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByCustomerOrderId(orderId);
    }

    // Get clean status history using DTO
    public List<OrderStatusHistoryDTO> getOrderStatusHistory(
            Long orderId) {

        if (!orderRepository.existsById(orderId)) {
            throw new IllegalArgumentException("Order not found");
        }

        return statusHistoryRepository
                .findByCustomerOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(history -> new OrderStatusHistoryDTO(
                        history.getId(),
                        history.getCustomerOrder().getId(),
                        history.getStatus(),
                        history.getChangedAt()
                ))
                .collect(Collectors.toList());
    }
}
