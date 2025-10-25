package com.example.feedback.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for authenticated users to change their password.
 */
public record PasswordChangeRequest(
        @JsonProperty("new_password")
        @NotBlank(message = "New password must not be blank")
        String newPassword
) {
}
