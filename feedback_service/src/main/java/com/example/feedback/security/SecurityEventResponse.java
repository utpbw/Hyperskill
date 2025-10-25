package com.example.feedback.security;

import java.time.Instant;

public record SecurityEventResponse(
        Instant date,
        SecurityEventAction action,
        String subject,
        String object,
        String path
) {
}
