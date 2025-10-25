package com.example.taskmanagement.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

@Service
public class TokenService {

    private final AccountUserRepository repository;
    private final String secret;
    private final Duration expiration;

    private SecretKey signingKey;

    public TokenService(AccountUserRepository repository,
                        @Value("${app.security.jwt.secret}") String secret,
                        @Value("${app.security.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.repository = repository;
        this.secret = secret;
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("JWT expiration must be positive");
        }
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issueToken(AccountUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        return Jwts.builder()
                .subject(user.getEmail().toLowerCase(Locale.ROOT))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("roles", user.getRoles().stream().map(Enum::name).toList())
                .signWith(signingKey)
                .compact();
    }

    public Authentication authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Missing bearer token");
        }

        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException("Token subject missing");
        }

        AccountUser user = repository.findByEmailIgnoreCase(subject)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.isLocked()) {
            throw new LockedException("User account is locked");
        }

        Collection<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toSet());

        return new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid bearer token", ex);
        }
    }
}
