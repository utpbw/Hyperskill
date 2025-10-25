package com.example.taskmanagement.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TaskCommentResponse(
        String id,
        @JsonProperty("task_id") String taskId,
        String text,
        String author
) {
}
