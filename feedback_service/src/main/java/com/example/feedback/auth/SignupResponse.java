package com.example.feedback.auth;

public record SignupResponse(
        String name,
        String lastname,
        String email
) {
}
