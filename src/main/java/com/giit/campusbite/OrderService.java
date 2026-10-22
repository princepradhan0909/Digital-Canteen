
package com.giit.campusbite;

import com.giit.campusbite.dto.OrderStatusHistoryDTO;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final FoodItemRepository foodRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;

    public OrderService(
            CustomerOrderRepository orderRepository,
            FoodItemRepository foodRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            OrderStatusHistoryRepository statusHistoryRepository) {

        this.orderRepository = orderRepository;
        this.foodRepository = foodRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    // Create a new order
    @Transactional
    public CustomerOrder createOrder(OrderRequest request) {

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

        if (!Set.of("COUNTER", "ONLINE").contains(paymentMethod)) {
            throw new IllegalArgumentException(
                    "Payment method must be COUNTER or ONLINE");
        }

        CustomerOrder order = new CustomerOrder(
                request.getStudentName().trim(),
                request.getStudentPhone().trim(),
                0.0
        );

        order.setOrderStatus("PLACED");
        order = orderRepository.save(order);

        double total = 0.0;
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

            double price = food.getPrice();
            int quantity = line.getQuantity();

            total += price * quantity;

            orderItems.add(new OrderItem(
                    order, food, quantity, price
            ));
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        orderItemRepository.saveAll(orderItems);

        Payment payment = new Payment(order, paymentMethod);
        payment.setPaymentStatus("PENDING");
        paymentRepository.save(payment);

        // Save initial order status in history
        statusHistoryRepository.save(
                new OrderStatusHistory(order, "PLACED")
        );

        return order;
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

        return savedOrder;
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
