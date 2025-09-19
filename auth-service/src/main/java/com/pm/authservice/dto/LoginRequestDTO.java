package com.pm.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Message is required")
    @Size(min = 8, message = "Password should be at least 8 characters long")
    private String password;

    public @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Message is required") @Size(min = 8, message = "Password should be at least 8 characters long") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Message is required") @Size(min = 8, message = "Password should be at least 8 characters long") String password) {
        this.password = password;
    }
}
