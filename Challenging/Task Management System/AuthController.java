package com.example.accounts.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenService tokenService;

    public AuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> createToken(Authentication authentication) {
        String username = Objects.requireNonNull(authentication, "authentication").getName();
        TokenService.Token token = tokenService.createTokenFor(username);
        return ResponseEntity.ok(new TokenResponse(token.token()));
    }

    public record TokenResponse(String token) {
    }
}
