package com.example.feedback.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotNull(message = "rating is required")
        @Min(value = 0, message = "rating must be non-negative")
        Integer rating,

        @Size(max = 1000, message = "feedback must be 1000 characters or fewer")
        String feedback,

        @Size(max = 255, message = "customer must be 255 characters or fewer")
        String customer,

        @NotBlank(message = "product is required")
        @Size(max = 255, message = "product must be 255 characters or fewer")
        String product,

        @NotBlank(message = "vendor is required")
        @Size(max = 255, message = "vendor must be 255 characters or fewer")
        String vendor
) {
}
