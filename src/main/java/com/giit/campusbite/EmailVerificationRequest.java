package com.giit.campusbite;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmailVerificationRequest {
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Pattern(regexp = "[0-9]{6}")
    private String code;

    public String getEmail() { return email; }
    public String getCode() { return code; }
    public void setEmail(String email) { this.email = email; }
    public void setCode(String code) { this.code = code; }
}
