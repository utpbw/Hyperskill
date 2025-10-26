package com.hyperskill.tracker;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/developers")
public class DeveloperController {

    private final DeveloperRepository developerRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;

    public DeveloperController(
        DeveloperRepository developerRepository,
        ApplicationRepository applicationRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.developerRepository = developerRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    @Transactional
    public ResponseEntity<Void> signUp(@Valid @RequestBody DeveloperRequest request) {
        String email = request.email();
        if (developerRepository.existsByEmail(email)) {
            throw new ResponseStatusException(BAD_REQUEST, "Email already registered");
        }

        Developer developer = new Developer(email, passwordEncoder.encode(request.password()));
        Developer saved = developerRepository.save(developer);

        return ResponseEntity
            .created(java.net.URI.create("/api/developers/" + saved.getId()))
            .build();
    }

    @GetMapping("/{id}")
    public DeveloperProfile getDeveloper(@PathVariable long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }

        Developer developer = developerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Developer not found"));

        if (!developer.getEmail().equals(authentication.getName())) {
            throw new ResponseStatusException(FORBIDDEN, "Forbidden");
        }

        List<DeveloperApplicationView> applications = applicationRepository.findAllByDeveloperOrderByIdDesc(developer)
            .stream()
            .map(app -> new DeveloperApplicationView(
                app.getId(),
                app.getName(),
                app.getDescription(),
                app.getApiKey(),
                app.getCategory()
            ))
            .toList();

        return new DeveloperProfile(developer.getId(), developer.getEmail(), applications);
    }
}
