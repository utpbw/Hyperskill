package com.example.accounts.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity, Long> {

    List<TaskCommentEntity> findAllByTaskOrderByIdDesc(TaskEntity task);

    long countByTask(TaskEntity task);

    @Query("""
            SELECT c.task.id AS taskId, COUNT(c.id) AS total
            FROM TaskCommentEntity c
            WHERE c.task.id IN :taskIds
            GROUP BY c.task.id
            """)
    List<CommentCount> findCommentCountsForTaskIds(List<Long> taskIds);

    interface CommentCount {
        Long getTaskId();

        Long getTotal();
    }
}
