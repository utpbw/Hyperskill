package com.example.taskmanagement.security;

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
