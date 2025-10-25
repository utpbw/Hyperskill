package com.example.feedback.security;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for persisting and retrieving security audit events.
 */
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
}
