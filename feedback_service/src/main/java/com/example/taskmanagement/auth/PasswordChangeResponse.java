package com.example.taskmanagement.auth;

public record PasswordChangeResponse(
        String email,
        String status
) {
}
