package com.hyperskill.tracker;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeveloperRepository extends JpaRepository<Developer, Long> {

    Optional<Developer> findByEmail(String email);

    boolean existsByEmail(String email);
}
