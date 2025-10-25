package com.example.taskmanagement.accounts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRegistrationRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        String password
) {
}
