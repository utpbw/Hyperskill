package com.example.feedback.auth;

/**
 * Response payload emitted after an administrator changes a user's lock state.
 */
public record UserAccessStatusResponse(String status) {
}
