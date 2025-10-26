package com.example.accounts.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean registerAccount(AccountRequest request) {
        String originalEmail = request.email().trim();
        String normalizedEmail = normalizeEmail(originalEmail);

        if (accountRepository.existsByNormalizedEmail(normalizedEmail)) {
            return false;
        }

        AccountEntity entity = new AccountEntity();
        entity.setEmail(originalEmail);
        entity.setNormalizedEmail(normalizedEmail);
        entity.setPassword(passwordEncoder.encode(request.password()));

        try {
            accountRepository.save(entity);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
