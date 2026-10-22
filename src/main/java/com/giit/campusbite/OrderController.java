
package com.giit.campusbite;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Create a new order
    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderRequest request) {

        try {
            CustomerOrder order = orderService.createOrder(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(order);

        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .badRequest()
                    .body(exception.getMessage());
        }
    }

    // Get an order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(
            @PathVariable Long orderId) {

        return orderService.getOrderById(orderId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    // Update order status
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody java.util.Map<String, String> request) {

        try {
            String status = request.get("status");

            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Status is required");
            }

            CustomerOrder order =
                    orderService.updateOrderStatus(orderId, status);

            return ResponseEntity.ok(order);

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(exception.getMessage());
        }
    }

    // Get order items
    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> getOrderItems(
            @PathVariable Long orderId) {

        try {
            return ResponseEntity.ok(
                    orderService.getOrderItems(orderId));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    // Get payment details
    @GetMapping("/{orderId}/payment")
    public ResponseEntity<?> getPayment(
            @PathVariable Long orderId) {

        return orderService.getPaymentByOrderId(orderId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    // Get clean order status history using DTO
    @GetMapping("/{orderId}/history")
    public ResponseEntity<?> getOrderStatusHistory(
            @PathVariable Long orderId) {

        try {
            return ResponseEntity.ok(
                    orderService.getOrderStatusHistory(orderId));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(exception.getMessage());
        }
    }
}
