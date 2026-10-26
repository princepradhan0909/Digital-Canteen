package com.giit.campusbite;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StudentUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthController(StudentUserRepository users,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          EmailVerificationService emailVerificationService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password must not exceed 72 UTF-8 bytes"));
        }
        if (request.getName() == null || request.getName().isBlank()
                || request.getPhone() == null || request.getPhone().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Name and phone are required"));
        }
        String email = request.getEmail().trim().toLowerCase();
        StudentUser user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user != null && user.isEmailVerified()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "An account with that email already exists"));
        }
        if (user != null && user.getVerificationSentAt() != null
                && user.getVerificationSentAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Wait one minute before requesting another code"));
        }
        if (user == null) {
            user = new StudentUser(
                    request.getName().trim(),
                    email,
                    request.getPhone().trim(),
                    passwordEncoder.encode(request.getPassword()),
                    "STUDENT");
        } else {
            user.setName(request.getName().trim());
            user.setPhone(request.getPhone().trim());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        issueVerificationCode(user);
        return ResponseEntity.accepted().body(Map.of(
                "message", "A six-digit verification code was sent to your email.",
                "email", user.getEmail()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {
        StudentUser user = users.findByEmailIgnoreCase(request.getEmail().trim())
                .orElse(null);
        if (user == null || user.isEmailVerified()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Verification code is invalid or expired"));
        }
        if (user.getVerificationCodeHash() == null
                || user.getVerificationAttempts() == null
                || user.getVerificationAttempts() >= 5
                || user.getVerificationExpiresAt() == null
                || user.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Verification code is invalid or expired"));
        }

        String suppliedHash = hashCode(request.getCode());
        if (!MessageDigest.isEqual(
                suppliedHash.getBytes(StandardCharsets.US_ASCII),
                user.getVerificationCodeHash().getBytes(StandardCharsets.US_ASCII))) {
            user.setVerificationAttempts(user.getVerificationAttempts() + 1);
            users.save(user);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Verification code is invalid or expired"));
        }

        user.setEmailVerified(true);
        user.setVerificationCodeHash(null);
        user.setVerificationExpiresAt(null);
        user.setVerificationAttempts(0);
        user = users.save(user);
        return ResponseEntity.ok(authResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        if (request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email or password is incorrect"));
        }
        StudentUser user = users.findByEmailIgnoreCase(request.getEmail().trim())
                .orElse(null);
        if (user == null || !passwordEncoder.matches(
                request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email or password is incorrect"));
        }
        if (!user.isEmailVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Verify your email before signing in"));
        }
        return ResponseEntity.ok(authResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        return users.findByEmailIgnoreCase(principal.getName())
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "phone", user.getPhone(),
                        "role", user.getRole())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @org.springframework.web.bind.annotation.PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            Principal principal, @Valid @RequestBody ProfileUpdateRequest request) {
        StudentUser user = users.findByEmailIgnoreCase(principal.getName())
                .orElseThrow();
        user.setName(request.getName().trim());
        user.setPhone(request.getPhone().trim());
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getNewPassword().length() < 8
                    || request.getNewPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "New password must be 8-72 bytes"));
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }
        user = users.save(user);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "role", user.getRole()));
    }

    private Map<String, Object> authResponse(StudentUser user) {
        return Map.of(
                "token", jwtService.issue(user),
                "tokenType", "Bearer",
                "expiresIn", 3600,
                "user", Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "phone", user.getPhone(),
                        "role", user.getRole()));
    }

    private void issueVerificationCode(StudentUser user) {
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        user.setVerificationCodeHash(hashCode(code));
        user.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setVerificationSentAt(LocalDateTime.now());
        user.setVerificationAttempts(0);
        users.save(user);
        emailVerificationService.sendCode(user.getEmail(), code);
    }

    private String hashCode(String code) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
