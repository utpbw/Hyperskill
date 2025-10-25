package com.example.feedback.auth;

import java.util.List;

/**
 * Representation of a user exposed to API consumers.
 */
public record UserResponse(
        Long id,
        String name,
        String lastname,
        String email,
        List<String> roles
) {
}
