package com.example.taskmanagement.tasks;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByAuthor(String author, Sort sort);

    List<Task> findAllByAssignee(String assignee, Sort sort);

    List<Task> findAllByAuthorAndAssignee(String author, String assignee, Sort sort);

    List<Task> findAllByAssigneeIsNull(Sort sort);

    List<Task> findAllByAuthorAndAssigneeIsNull(String author, Sort sort);
}
