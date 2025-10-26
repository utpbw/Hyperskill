package com.example.accounts.api;

import jakarta.validation.constraints.NotBlank;

public record TaskStatusRequest(
        @NotBlank(message = "status is required")
        String status
) {
}
