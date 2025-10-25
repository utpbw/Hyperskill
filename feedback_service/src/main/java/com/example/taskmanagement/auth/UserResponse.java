package com.example.taskmanagement.auth;

import java.util.List;

public record UserResponse(
        Long id,
        String name,
        String lastname,
        String email,
        List<String> roles
) {
}
