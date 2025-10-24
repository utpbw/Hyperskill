package com.example.feedback.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FeedbackPageResponse(
        @JsonProperty("total_documents") long totalDocuments,
        @JsonProperty("is_first_page") boolean firstPage,
        @JsonProperty("is_last_page") boolean lastPage,
        List<FeedbackResponse> documents
) {
}
