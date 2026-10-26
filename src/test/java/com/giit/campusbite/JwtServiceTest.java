package com.giit.campusbite;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "unit-test-secret-that-is-at-least-thirty-two-characters",
            new ObjectMapper());

    @Test
    void issuesAndValidatesStudentToken() {
        StudentUser user = new StudentUser(
                "Student", "student@example.edu", "1234567890",
                "not-a-real-password-hash", "STUDENT");

        var claims = jwtService.validateAndRead(jwtService.issue(user));

        assertEquals("student@example.edu", claims.get("sub"));
        assertEquals("STUDENT", claims.get("role"));
    }

    @Test
    void rejectsModifiedToken() {
        StudentUser user = new StudentUser(
                "Student", "student@example.edu", "1234567890",
                "not-a-real-password-hash", "STUDENT");
        String token = jwtService.issue(user);
        String modified = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertNull(jwtService.validateAndRead(modified));
    }
}
