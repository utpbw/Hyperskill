package com.example.accounts.api;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity, Long> {

    List<TaskCommentEntity> findAllByTaskOrderByIdDesc(TaskEntity task);
}
