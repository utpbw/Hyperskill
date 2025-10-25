package com.example.taskmanagement.tasks;

import jakarta.validation.constraints.NotBlank;

public record TaskAssignmentRequest(@NotBlank String assignee) {
}
