package com.example.taskmanagement.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank(message = "Name must not be blank")
        String name,

        @NotBlank(message = "Lastname must not be blank")
        String lastname,

        @NotBlank(message = "Email must not be blank")
        @Email(
                regexp = "(?i)^.+@acme\\.com$",
                message = "Email must belong to the corporate domain"
        )
        String email,

        @NotBlank(message = "Password must not be blank")
        String password
) {
}
