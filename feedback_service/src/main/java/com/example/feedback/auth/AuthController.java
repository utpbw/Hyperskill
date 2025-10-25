package com.example.feedback.auth;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AccountUserService accountUserService;

    public AuthController(AccountUserService accountUserService) {
        this.accountUserService = accountUserService;
    }

    @PostMapping(path = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return accountUserService.registerUser(request);
    }
}
