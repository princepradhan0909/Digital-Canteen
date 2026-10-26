package com.giit.campusbite;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${campusbite.jwt-secret:}") String secret,
            ObjectMapper objectMapper) {
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be configured with at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    public String issue(StudentUser user) {
        try {
            String header = ENCODER.encodeToString(
                    objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(Map.of(
                    "sub", user.getEmail(),
                    "role", user.getRole(),
                    "name", user.getName(),
                    "iat", Instant.now().getEpochSecond(),
                    "exp", Instant.now().plusSeconds(3600).getEpochSecond()
            )));
            String content = header + "." + payload;
            return content + "." + ENCODER.encodeToString(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not issue access token", exception);
        }
    }

    public Map<String, Object> validateAndRead(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) return null;

            Map<?, ?> header = objectMapper.readValue(DECODER.decode(parts[0]), Map.class);
            if (!"HS256".equals(header.get("alg"))) return null;

            byte[] suppliedSignature = DECODER.decode(parts[2]);
            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!java.security.MessageDigest.isEqual(suppliedSignature, expectedSignature)) {
                return null;
            }

            Map<String, Object> claims = objectMapper.readValue(
                    DECODER.decode(parts[1]), new TypeReference<>() {});
            Object expiration = claims.get("exp");
            if (!(expiration instanceof Number) ||
                    ((Number) expiration).longValue() <= Instant.now().getEpochSecond()) {
                return null;
            }
            if (!(claims.get("sub") instanceof String) ||
                    !(claims.get("role") instanceof String)) {
                return null;
            }
            return claims;
        } catch (Exception exception) {
            return null;
        }
    }

    private byte[] sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
