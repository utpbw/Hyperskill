package com.example.taskmanagement.tasks;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    List<TaskComment> findAllByTask(Task task, Sort sort);

    long countByTask(Task task);

    @Query("select tc.task.id as taskId, count(tc.id) as total " +
            "from TaskComment tc where tc.task.id in :taskIds group by tc.task.id")
    List<TaskCommentCount> countAllByTaskIds(@Param("taskIds") List<Long> taskIds);
}
