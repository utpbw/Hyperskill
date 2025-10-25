package com.example.taskmanagement.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TaskResponse(
        String id,
        String title,
        String description,
        String status,
        String author,
        String assignee,
        @JsonProperty("total_comments") long totalComments
) {
}
