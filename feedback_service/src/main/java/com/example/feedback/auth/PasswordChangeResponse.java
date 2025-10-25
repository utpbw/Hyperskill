package com.example.feedback.auth;

public record PasswordChangeResponse(
        String email,
        String status
) {
}
