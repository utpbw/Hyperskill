package com.example.accounts.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final TokenRepository tokenRepository;
    private final AccountRepository accountRepository;

    public TokenService(TokenRepository tokenRepository, AccountRepository accountRepository) {
        this.tokenRepository = tokenRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Token createTokenFor(String email) {
        String normalizedEmail = normalizeEmail(email);
        AccountEntity account = accountRepository.findByNormalizedEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setToken(UUID.randomUUID().toString());
        tokenEntity.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        tokenEntity.setAccount(account);

        TokenEntity saved = tokenRepository.save(tokenEntity);
        return new Token(saved.getToken(), saved.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public Optional<String> findNormalizedEmailByToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return Optional.empty();
        }

        return tokenRepository.findByToken(tokenValue)
                .filter(entity -> entity.getExpiresAt().isAfter(Instant.now()))
                .map(entity -> entity.getAccount().getNormalizedEmail());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public record Token(String token, Instant expiresAt) {
    }
}
