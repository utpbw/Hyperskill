package com.example.taskmanagement.tasks;

import jakarta.validation.constraints.NotBlank;

public record TaskCommentRequest(@NotBlank(message = "Comment text must not be blank") String text) {
}
