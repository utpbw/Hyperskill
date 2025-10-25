package com.example.feedback.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<AccountUser> findByEmailIgnoreCase(String email);
}
