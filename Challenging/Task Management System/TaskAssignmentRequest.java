package com.example.accounts.api;

import jakarta.validation.constraints.NotBlank;

public record TaskAssignmentRequest(
        @NotBlank(message = "assignee is required")
        String assignee
) {
}
