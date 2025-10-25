package com.example.accounts.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @PostMapping
    public ResponseEntity<?> createAccount(@Validated @RequestBody AccountRequest request) {
        // If validation passes, return OK
        return ResponseEntity.ok().build();
    }
}
