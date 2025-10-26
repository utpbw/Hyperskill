package com.example.accounts.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private static final String CREATED_STATUS = "CREATED";

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Task createTask(TaskRequest request, String authorEmail) {
        String normalizedAuthor = normalizeEmail(authorEmail);

        TaskEntity entity = new TaskEntity();
        entity.setTitle(request.title().trim());
        entity.setDescription(request.description().trim());
        entity.setStatus(CREATED_STATUS);
        entity.setAuthorEmail(normalizedAuthor);

        TaskEntity saved = taskRepository.save(entity);
        return toTask(saved);
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks(String authorEmail) {
        List<TaskEntity> entities;
        if (authorEmail != null && !authorEmail.isBlank()) {
            entities = taskRepository.findAllByAuthorEmailOrderByIdDesc(normalizeEmail(authorEmail));
        } else {
            entities = taskRepository.findAllByOrderByIdDesc();
        }
        return entities.stream().map(this::toTask).collect(Collectors.toList());
    }

    private Task toTask(TaskEntity entity) {
        return new Task(
                String.valueOf(entity.getId()),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getAuthorEmail(),
                "none"
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public record Task(String id, String title, String description, String status, String author, String assignee) { }
}
