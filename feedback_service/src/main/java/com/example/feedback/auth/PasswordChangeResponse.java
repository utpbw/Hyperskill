package com.example.feedback.auth;

/**
 * Response wrapper returned after a successful password update operation.
 */
public record PasswordChangeResponse(
        String email,
        String status
) {
}
