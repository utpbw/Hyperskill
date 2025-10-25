package com.example.feedback.auth;

/**
 * Response payload returned after an administrator deletes a user account.
 */
public record UserDeletionResponse(
        String user,
        String status
) {
}
