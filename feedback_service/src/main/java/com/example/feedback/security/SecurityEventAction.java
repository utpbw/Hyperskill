package com.example.feedback.security;

/**
 * Enumeration of audit event types emitted by the security subsystem.
 */
public enum SecurityEventAction {
    CREATE_USER,
    CHANGE_PASSWORD,
    ACCESS_DENIED,
    LOGIN_FAILED,
    BRUTE_FORCE,
    LOCK_USER,
    UNLOCK_USER,
    DELETE_USER,
    GRANT_ROLE,
    REMOVE_ROLE
}
