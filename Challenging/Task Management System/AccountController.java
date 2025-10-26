package com.example.accounts.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Void> createAccount(@Valid @RequestBody AccountRequest request) {
        boolean created = accountService.registerAccount(request);

        if (!created) {
            return ResponseEntity.status(409).build();
        }

        return ResponseEntity.ok().build();
    }
}
