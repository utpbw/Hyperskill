package com.example.accounts.api;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity, Long> {
}
