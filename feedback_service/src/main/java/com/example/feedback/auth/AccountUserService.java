package com.example.feedback.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AccountUserService {

    private final AccountUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AccountUserService(AccountUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse registerUser(SignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String sanitizedName = request.name().trim();
        String sanitizedLastname = request.lastname().trim();
        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User exist!");
        }

        AccountUser user = new AccountUser();
        user.setName(sanitizedName);
        user.setLastname(sanitizedLastname);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));

        AccountUser saved = repository.save(user);
        return new SignupResponse(saved.getId(), saved.getName(), saved.getLastname(), saved.getEmail());
    }

    @Transactional(readOnly = true)
    public AccountUser findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
