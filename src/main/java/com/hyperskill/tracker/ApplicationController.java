package com.hyperskill.tracker;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final DeveloperRepository developerRepository;

    public ApplicationController(ApplicationRepository applicationRepository, DeveloperRepository developerRepository) {
        this.applicationRepository = applicationRepository;
        this.developerRepository = developerRepository;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApplicationRegistrationResponse> register(
        @Valid @RequestBody ApplicationRegistrationRequest request,
        Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }

        if (applicationRepository.existsByName(request.name())) {
            throw new ResponseStatusException(BAD_REQUEST, "Application name already registered");
        }

        Developer developer = developerRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unauthorized"));

        String apiKey = generateUniqueApiKey();
        Application application = new Application(
            request.name(),
            request.description(),
            apiKey,
            request.category(),
            developer
        );
        Application saved = applicationRepository.save(application);
        ApplicationRegistrationResponse response = new ApplicationRegistrationResponse(
            saved.getName(),
            saved.getApiKey(),
            saved.getCategory()
        );

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
