package com.example.feedback.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(
        @NotBlank(message = "Name must not be blank")
        String name,

        @NotBlank(message = "Lastname must not be blank")
        String lastname,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid address")
        @Pattern(
                regexp = "(?i).+@acme\\.com$",
                message = "Email must belong to the corporate domain"
        )
        String email,

        @NotBlank(message = "Password must not be blank")
        String password
) {
}
