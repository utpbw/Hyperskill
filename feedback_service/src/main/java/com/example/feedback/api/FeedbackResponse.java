package com.example.feedback.api;

import com.example.feedback.data.FeedbackDocument;

/**
 * DTO representing feedback documents returned to API consumers.
 */
public record FeedbackResponse(
        String id,
        Integer rating,
        String feedback,
        String customer,
        String product,
        String vendor
) {
    public static FeedbackResponse fromDocument(FeedbackDocument document) {
        return new FeedbackResponse(
                document.getId(),
                document.getRating(),
                document.getFeedback(),
                document.getCustomer(),
                document.getProduct(),
                document.getVendor()
        );
    }
}
