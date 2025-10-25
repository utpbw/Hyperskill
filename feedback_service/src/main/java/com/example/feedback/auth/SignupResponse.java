package com.example.feedback.auth;

public record SignupResponse(
        Long id,
        String name,
        String lastname,
        String email
) {
}
