package com.example.taskmanagement.accounts;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private final AccountRegistrationService accountRegistrationService;

    public AccountController(AccountRegistrationService accountRegistrationService) {
        this.accountRegistrationService = accountRegistrationService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountRegistrationResponse register(@Valid @RequestBody AccountRegistrationRequest request) {
        return accountRegistrationService.registerAccount(request);
    }
}
