package com.hyperskill.tracker;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;

    public ApplicationController(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApplicationRegistrationResponse> register(
        @Valid @RequestBody ApplicationRegistrationRequest request
    ) {
        if (applicationRepository.existsByName(request.name())) {
            throw new ResponseStatusException(BAD_REQUEST, "Application name already registered");
        }

        String apiKey = generateUniqueApiKey();
        Application application = new Application(request.name(), request.description(), apiKey);
        Application saved = applicationRepository.save(application);
        ApplicationRegistrationResponse response = new ApplicationRegistrationResponse(saved.getName(), saved.getApiKey());

        return ResponseEntity.status(CREATED).body(response);
    }

    private String generateUniqueApiKey() {
        String apiKey;
        do {
            apiKey = UUID.randomUUID().toString();
        } while (applicationRepository.existsByApiKey(apiKey));
        return apiKey;
    }
}
