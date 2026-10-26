package com.giit.campusbite;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final FoodItemRepository foods;
    private final FoodCategoryRepository categories;
    private final CustomerOrderRepository orders;
    private final PaymentRepository payments;

    public AdminController(FoodItemRepository foods,
                           FoodCategoryRepository categories,
                           CustomerOrderRepository orders,
                           PaymentRepository payments) {
        this.foods = foods;
        this.categories = categories;
        this.orders = orders;
        this.payments = payments;
    }

    @GetMapping("/foods")
    public ResponseEntity<?> foods() {
        return ResponseEntity.ok(foods.findAll());
    }

    @PostMapping("/foods")
    @Transactional
    public ResponseEntity<?> createFood(@Valid @RequestBody FoodItem food) {
        food.setFoodCategory(findOrCreateCategory(food.getCategory()));
        food.setPrice(food.getPrice());
        food.setAvailable(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(foods.save(food));
    }

    @PutMapping("/foods/{id}")
    @Transactional
    public ResponseEntity<?> updateFood(
            @PathVariable Long id, @Valid @RequestBody FoodItem update) {
        return foods.findById(id).<ResponseEntity<?>>map(food -> {
            food.setName(update.getName());
            food.setFoodCategory(findOrCreateCategory(update.getCategory()));
            food.setPrice(update.getPrice());
            food.setImageUrl(update.getImageUrl());
            food.setAvailable(update.isAvailable());
            return ResponseEntity.ok(foods.save(food));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private FoodCategory findOrCreateCategory(String name) {
        String normalizedName = name.trim().replaceAll("\\s+", " ");
        return categories.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> categories.save(new FoodCategory(
                        normalizedName, null, (int) categories.count())));
    }

    @DeleteMapping("/foods/{id}")
    public ResponseEntity<?> deleteFood(@PathVariable Long id) {
        return foods.findById(id).<ResponseEntity<?>>map(food -> {
            food.setAvailable(false);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders() {
        return ResponseEntity.ok(orders.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/reports/sales")
    public ResponseEntity<?> salesReport(@RequestParam(defaultValue = "daily") String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = switch (period.toLowerCase()) {
            case "daily" -> today;
            case "weekly" -> today.with(TemporalAdjusters.previousOrSame(
                    java.time.DayOfWeek.MONDAY));
            case "monthly" -> today.withDayOfMonth(1);
            default -> null;
        };
        if (startDate == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Period must be daily, weekly, or monthly"));
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        var matchingOrders = orders
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end);
        var paidOrders = matchingOrders.stream()
                .filter(order -> !"CANCELLED".equals(order.getOrderStatus()))
                .filter(order -> payments.findByCustomerOrderId(order.getId())
                        .map(payment -> "PAID".equals(payment.getPaymentStatus()))
                        .orElse(false))
                .toList();
        double collectedSales = paidOrders.stream()
                .mapToDouble(CustomerOrder::getTotalAmount).sum();
        double pendingSales = matchingOrders.stream()
                .filter(order -> !"CANCELLED".equals(order.getOrderStatus()))
                .mapToDouble(CustomerOrder::getTotalAmount).sum() - collectedSales;
        return ResponseEntity.ok(Map.of(
                "period", period.toLowerCase(),
                "from", startDate,
                "to", today,
                "orderCount", matchingOrders.size(),
                "grossSales", collectedSales,
                "pendingSales", pendingSales,
                "paidOrderCount", paidOrders.size()));
    }
}
