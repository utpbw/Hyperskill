package com.example.feedback.security;

import java.time.Instant;

/**
 * Projection returned by the security events API describing recorded audit entries.
 */
public record SecurityEventResponse(
        Long id,
        Instant date,
        SecurityEventAction action,
        String subject,
        String object,
        String path
) {
}
