package com.giit.campusbite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_users")
public class StudentUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = "STUDENT";

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean emailVerified = true;

    private String verificationCodeHash;
    private LocalDateTime verificationExpiresAt;
    private LocalDateTime verificationSentAt;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer verificationAttempts = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudentUser() {
    }

    public StudentUser(String name, String email, String phone, String passwordHash,
                       String role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.emailVerified = "ADMIN".equals(role);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getVerificationCodeHash() { return verificationCodeHash; }
    public LocalDateTime getVerificationExpiresAt() { return verificationExpiresAt; }
    public LocalDateTime getVerificationSentAt() { return verificationSentAt; }
    public Integer getVerificationAttempts() { return verificationAttempts; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(String role) { this.role = role; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setVerificationCodeHash(String verificationCodeHash) { this.verificationCodeHash = verificationCodeHash; }
    public void setVerificationExpiresAt(LocalDateTime verificationExpiresAt) { this.verificationExpiresAt = verificationExpiresAt; }
    public void setVerificationSentAt(LocalDateTime verificationSentAt) { this.verificationSentAt = verificationSentAt; }
    public void setVerificationAttempts(Integer verificationAttempts) { this.verificationAttempts = verificationAttempts; }
}
