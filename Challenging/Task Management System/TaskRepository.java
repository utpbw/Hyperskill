package com.example.accounts.api;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findAllByOrderByIdDesc();

    List<TaskEntity> findAllByAuthorEmailOrderByIdDesc(String authorEmail);
}
