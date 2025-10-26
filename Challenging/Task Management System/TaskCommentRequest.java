package com.example.accounts.api;

import jakarta.validation.constraints.NotBlank;

public record TaskCommentRequest(
        @NotBlank(message = "text is required")
        String text
) {
}
