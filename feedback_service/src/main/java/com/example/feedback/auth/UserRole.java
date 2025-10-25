package com.example.feedback.auth;

import java.util.Locale;
import java.util.Optional;

public enum UserRole {
    ROLE_ADMINISTRATOR(true),
    ROLE_ACCOUNTANT(false),
    ROLE_USER(false),
    ROLE_AUDITOR(false);

    private final boolean administrative;

    UserRole(boolean administrative) {
        this.administrative = administrative;
    }

    public boolean isAdministrative() {
        return administrative;
    }

    public boolean isBusiness() {
        return !administrative;
    }

    public String getRoleName() {
        return name().replace("ROLE_", "");
    }

    public static Optional<UserRole> fromName(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        String upperCase = normalized.toUpperCase(Locale.ROOT);
        if (!upperCase.startsWith("ROLE_")) {
            upperCase = "ROLE_" + upperCase;
        }
        try {
            return Optional.of(UserRole.valueOf(upperCase));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
