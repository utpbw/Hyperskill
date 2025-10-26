package com.example.accounts.api;

import jakarta.validation.constraints.NotBlank;

public record TaskRequest(
        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "description is required")
        String description
) {
}
