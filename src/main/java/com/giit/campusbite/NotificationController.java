package com.giit.campusbite;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> list(Principal principal) {
        return ResponseEntity.ok(notificationService.listFor(principal.getName()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markRead(
            Principal principal, @PathVariable Long notificationId) {
        try {
            return ResponseEntity.ok(notificationService.markRead(
                    principal.getName(), notificationId));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}
