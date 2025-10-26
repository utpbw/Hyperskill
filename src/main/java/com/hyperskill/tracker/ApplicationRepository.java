package com.hyperskill.tracker;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByName(String name);

    boolean existsByApiKey(String apiKey);

    List<Application> findAllByDeveloperOrderByIdDesc(Developer developer);
}
