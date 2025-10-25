package com.example.accounts.api;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AccountService {

    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

    public boolean registerAccount(AccountRequest request) {
        String originalEmail = request.email().trim();
        String normalizedEmail = normalizeEmail(originalEmail);
        Account account = new Account(originalEmail, request.password());
        return accounts.putIfAbsent(normalizedEmail, account) == null;
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    public record Account(String email, String password) { }
}
