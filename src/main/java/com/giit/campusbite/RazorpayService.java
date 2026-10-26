package com.giit.campusbite;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
public class RazorpayService {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    public RazorpayService(
            @Value("${campusbite.razorpay-key-id:}") String keyId,
            @Value("${campusbite.razorpay-key-secret:}") String keySecret,
            @Value("${campusbite.razorpay-webhook-secret:}") String webhookSecret,
            ObjectMapper objectMapper) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> createOrder(Payment payment) {
        requireConfigured();
        long amount = Math.round(payment.getCustomerOrder().getTotalAmount() * 100);
        if (amount < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Online payments require a positive order total");
        }
        if (payment.getGatewayOrderId() != null
                && !payment.getGatewayOrderId().isBlank()) {
            return Map.of(
                    "key", keyId,
                    "gatewayOrderId", payment.getGatewayOrderId(),
                    "amount", amount,
                    "currency", "INR",
                    "name", "CampusBite",
                    "description", "Campus canteen order #" +
                            payment.getCustomerOrder().getId());
        }

        try {
            String credentials = Base64.getEncoder().encodeToString(
                    (keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            String payload = objectMapper.writeValueAsString(Map.of(
                    "amount", amount,
                    "currency", "INR",
                    "receipt", "campusbite-" + payment.getCustomerOrder().getId(),
                    "notes", Map.of("campusbite_order_id",
                            payment.getCustomerOrder().getId().toString())));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Razorpay could not create the payment order");
            }
            JsonNode body = objectMapper.readTree(response.body());
            String gatewayOrderId = body.path("id").asText("");
            if (gatewayOrderId.isBlank() || body.path("amount").asLong(-1) != amount) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Razorpay returned an invalid payment order");
            }
            payment.setGatewayOrderId(gatewayOrderId);
            return Map.of(
                    "key", keyId,
                    "gatewayOrderId", gatewayOrderId,
                    "amount", amount,
                    "currency", "INR",
                    "name", "CampusBite",
                    "description", "Campus canteen order #" +
                            payment.getCustomerOrder().getId());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Payment provider request was interrupted", exception);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not create a Razorpay payment order", exception);
        }
    }

    public void verify(Payment payment, String gatewayOrderId,
                       String gatewayPaymentId, String signature) {
        requireConfigured();
        if (gatewayOrderId == null || gatewayPaymentId == null
                || signature == null
                || !gatewayOrderId.equals(payment.getGatewayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment details do not match this order");
        }
        if ("PAID".equals(payment.getPaymentStatus())) return;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(
                    (gatewayOrderId + "|" + gatewayPaymentId)
                            .getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(normalizeSignature(signature));
            if (!MessageDigest.isEqual(expected, supplied)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Payment signature is invalid");
            }
            payment.setGatewayPaymentId(gatewayPaymentId);
            payment.setPaymentStatus("PAID");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Payment signature is invalid", exception);
        }
    }

    public void requireConfigured() {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Online payments are not configured. Use counter payment or contact the canteen.");
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        if (webhookSecret.isBlank() || signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(normalizeSignature(signature));
            return MessageDigest.isEqual(expected, supplied);
        } catch (Exception exception) {
            return false;
        }
    }

    private String normalizeSignature(String signature) {
        if (signature == null) {
            return null;
        }
        String normalized = signature.trim();
        if (normalized.regionMatches(true, 0, "sha256=", 0, 7)) {
            normalized = normalized.substring(7);
        }
        return normalized;
    }
}
