package com.giit.campusbite;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RazorpayServiceTest {

    @Test
    void validatesWebhookSignatureAndRejectsTampering() throws Exception {
        String secret = "webhook-test-secret";
        String body = "{\"event\":\"payment.captured\"}";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(
                mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        RazorpayService service = new RazorpayService(
                "key-id", "key-secret", secret, new ObjectMapper());

        assertTrue(service.verifyWebhookSignature(body, signature));
        assertFalse(service.verifyWebhookSignature(body + " ", signature));
    }
}
