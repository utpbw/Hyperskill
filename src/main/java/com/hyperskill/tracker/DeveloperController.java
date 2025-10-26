package com.hyperskill.tracker;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/developers")
public class DeveloperController {

    private final AtomicLong idSequence = new AtomicLong(1L);
    private final Map<String, Developer> developers = new ConcurrentHashMap<>();

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody DeveloperRequest request) {
        String email = request.email();
        if (developers.containsKey(email)) {
            throw new ResponseStatusException(BAD_REQUEST, "Email already registered");
        }

        long id = idSequence.getAndIncrement();
        developers.put(email, new Developer(id, email, request.password()));

        URI location = URI.create("/api/developers/" + id);
        return ResponseEntity.created(location).build();
    }
}
