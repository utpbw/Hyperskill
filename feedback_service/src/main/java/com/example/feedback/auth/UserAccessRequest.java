package com.example.feedback.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload used by administrators to lock or unlock a user account.
 */
public record UserAccessRequest(
        @NotBlank(message = "User email is required") String user,
        @NotNull(message = "Operation is required") UserAccessOperation operation
) {
}
