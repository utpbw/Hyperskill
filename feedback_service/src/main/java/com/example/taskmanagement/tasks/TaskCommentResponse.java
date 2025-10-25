package com.example.taskmanagement.tasks;

public record TaskCommentResponse(
        String id,
        String taskId,
        String text,
        String author,
        String createdAt
) {
}
