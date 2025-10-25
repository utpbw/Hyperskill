package com.example.taskmanagement.accounts;

import com.example.taskmanagement.auth.AccountUser;
import com.example.taskmanagement.auth.AccountUserRepository;
import com.example.taskmanagement.auth.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Locale;

@Service
public class AccountRegistrationService {

    private final AccountUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AccountRegistrationService(AccountUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountRegistrationResponse registerAccount(AccountRegistrationRequest request) {
        String trimmedEmail = request.email().trim();
        if (trimmedEmail.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must not be blank");
        }

        String normalizedEmail = trimmedEmail.toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account with this email already exists");
        }

        AccountUser user = new AccountUser();
        user.setName("Account");
        user.setLastname("User");
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(EnumSet.of(UserRole.ROLE_USER));

        AccountUser saved = repository.save(user);
        return new AccountRegistrationResponse(saved.getEmail());
    }
}
