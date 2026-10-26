package com.giit.campusbite;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayWebhookController {

    private final RazorpayService razorpayService;
    private final PaymentRepository payments;
    private final CustomerOrderRepository orders;
    private final NotificationService notifications;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookController(RazorpayService razorpayService,
                                     PaymentRepository payments,
                                     CustomerOrderRepository orders,
                                     NotificationService notifications,
                                     ObjectMapper objectMapper) {
        this.razorpayService = razorpayService;
        this.payments = payments;
        this.orders = orders;
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<?> webhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload) {
        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid webhook signature"));
        }

        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (JacksonException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid webhook payload"));
        }
        if (!"payment.captured".equals(event.path("event").asText())) {
            return ResponseEntity.ok(Map.of("received", true));
        }
        JsonNode paymentEntity = event.path("payload").path("payment")
                .path("entity");
        String gatewayOrderId = paymentEntity.path("order_id").asText("");
        String gatewayPaymentId = paymentEntity.path("id").asText("");
        long paidAmount = paymentEntity.path("amount").asLong(-1);
        Payment payment = payments.findByGatewayOrderId(gatewayOrderId)
                .orElse(null);
        if (payment == null || gatewayOrderId.isBlank()
                || gatewayPaymentId.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        long expectedAmount = Math.round(
                payment.getCustomerOrder().getTotalAmount() * 100);
        if (paidAmount != expectedAmount) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Captured amount does not match the order"));
        }
        if (!"PAID".equals(payment.getPaymentStatus())) {
            payment.setGatewayPaymentId(gatewayPaymentId);
            payment.setPaymentStatus("PAID");
            CustomerOrder order = payment.getCustomerOrder();
            if ("PLACED".equals(order.getOrderStatus())) {
                order.setOrderStatus("PREPARING");
                orders.save(order);
            }
            payments.save(payment);
            notifications.notifyOrder(order,
                    "Payment for order #" + order.getId()
                            + " was successful.");
        }
        return ResponseEntity.ok(Map.of("received", true));
    }
}
