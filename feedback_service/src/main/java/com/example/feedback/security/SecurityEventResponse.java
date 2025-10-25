package com.example.feedback.security;

import java.time.Instant;

public record SecurityEventResponse(
        Long id,
        Instant date,
        SecurityEventAction action,
        String subject,
        String object,
        String path
) {
}
