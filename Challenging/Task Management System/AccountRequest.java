package com.example.accounts.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRequest(
    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    String email,

    @NotBlank(message = "password is required")
    @Size(min = 6, message = "password must be at least 6 characters")
    String password
) {}
