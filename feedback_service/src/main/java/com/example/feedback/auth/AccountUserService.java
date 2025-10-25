package com.example.feedback.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class AccountUserService {

    private final AccountUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> BREACHED_PASSWORDS = Set.of(
            "PasswordForJanuary",
            "PasswordForFebruary",
            "PasswordForMarch",
            "PasswordForApril",
            "PasswordForMay",
            "PasswordForJune",
            "PasswordForJuly",
            "PasswordForAugust",
            "PasswordForSeptember",
            "PasswordForOctober",
            "PasswordForNovember",
            "PasswordForDecember"
    );

    public AccountUserService(AccountUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse registerUser(SignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String sanitizedName = request.name().trim();
        String sanitizedLastname = request.lastname().trim();
        validatePassword(request.password());
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

    @Transactional
    public PasswordChangeResponse changePassword(String email, String newPassword) {
        AccountUser user = findByEmail(email);
        validatePassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The passwords must be different!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        return new PasswordChangeResponse(user.getEmail(), "The password has been updated successfully");
    }

    @Transactional(readOnly = true)
    public AccountUser findByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private void validatePassword(String password) {
        if (password.length() < 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password length must be 12 chars minimum!");
        }

        if (BREACHED_PASSWORDS.contains(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is in the hacker's database!");
        }
    }
}
