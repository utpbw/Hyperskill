package com.example.taskmanagement.tasks;

import jakarta.validation.constraints.NotBlank;

public record TaskRequest(
        @NotBlank String title,
        @NotBlank String description
) {
}
