package com.example.feedback.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotBlank(message = "User must not be blank")
        @Email(message = "User must be a valid email")
        String user,

        @NotBlank(message = "Role must not be blank")
        String role,

        @NotNull(message = "Operation must not be null")
        RoleOperation operation
) {
}
