package com.giit.campusbite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 20)
    private String phone;

    @Size(min = 8, max = 72)
    private String newPassword;

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getNewPassword() { return newPassword; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
