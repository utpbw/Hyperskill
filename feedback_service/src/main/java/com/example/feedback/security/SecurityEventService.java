package com.example.feedback.security;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class SecurityEventService {

    public static final String ANONYMOUS_SUBJECT = "Anonymous";

    private final SecurityEventRepository repository;

    public SecurityEventService(SecurityEventRepository repository) {
        this.repository = repository;
    }

    public void logEvent(SecurityEventAction action, String subject, String object, String path) {
        SecurityEvent event = new SecurityEvent();
        event.setAction(action);
        event.setSubject(normalizeSubject(subject));
        event.setObject(object);
        event.setPath(path);
        event.setDate(Instant.now());
        repository.save(event);
    }

    public List<SecurityEventResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(event -> new SecurityEventResponse(
                        event.getDate(),
                        event.getAction(),
                        event.getSubject(),
                        event.getObject(),
                        event.getPath()
                ))
                .toList();
    }

    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return ANONYMOUS_SUBJECT;
        }
        String trimmed = subject.trim();
        if (ANONYMOUS_SUBJECT.equalsIgnoreCase(trimmed)) {
            return ANONYMOUS_SUBJECT;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
