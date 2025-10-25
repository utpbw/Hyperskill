package com.example.accounts.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final Set<String> registeredEmails = ConcurrentHashMap.newKeySet();

    @PostMapping
    public ResponseEntity<?> createAccount(@Validated @RequestBody AccountRequest request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);

        boolean isNewEmail = registeredEmails.add(normalizedEmail);
        if (!isNewEmail) {
            return ResponseEntity.status(409).build();
        }

        // If validation passes, return OK
        return ResponseEntity.ok().build();
    }
}
