package com.example.accounts.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TaskService {

    private static final String CREATED_STATUS = "CREATED";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final TaskRepository taskRepository;
    private final AccountRepository accountRepository;

    public TaskService(TaskRepository taskRepository, AccountRepository accountRepository) {
        this.taskRepository = taskRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Task createTask(TaskRequest request, String authorEmail) {
        String normalizedAuthor = normalizeEmail(authorEmail);

        TaskEntity entity = new TaskEntity();
        entity.setTitle(request.title().trim());
        entity.setDescription(request.description().trim());
        entity.setStatus(CREATED_STATUS);
        entity.setAuthorEmail(normalizedAuthor);
        entity.setAssigneeEmail(null);

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

    @Transactional
    public Task assignTask(long taskId, String assigneeValue, String actingUserEmail) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        String normalizedActor = normalizeEmail(actingUserEmail);
        if (!task.getAuthorEmail().equals(normalizedActor)) {
            throw new ResponseStatusException(FORBIDDEN);
        }

        String processedAssignee = normalizeOptionalEmail(assigneeValue)
                .orElse(null);

        if (processedAssignee == null && !isNoneAssignee(assigneeValue)) {
            throw new ResponseStatusException(BAD_REQUEST);
        }

        if (processedAssignee != null) {
            boolean accountExists = accountRepository.existsByNormalizedEmail(processedAssignee);
            if (!accountExists) {
                throw new ResponseStatusException(NOT_FOUND);
            }
        }

        task.setAssigneeEmail(processedAssignee);
        TaskEntity saved = taskRepository.save(task);
        return toTask(saved);
    }

    private Task toTask(TaskEntity entity) {
        return new Task(
                String.valueOf(entity.getId()),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getAuthorEmail(),
                entity.getAssigneeEmail() == null ? "none" : entity.getAssigneeEmail()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private Optional<String> normalizeOptionalEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }

        String trimmed = email.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new ResponseStatusException(BAD_REQUEST);
        }

        if (isNoneAssignee(trimmed)) {
            return Optional.empty();
        }

        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ResponseStatusException(BAD_REQUEST);
        }

        return Optional.of(normalizeEmail(trimmed));
    }

    private boolean isNoneAssignee(String value) {
        return value != null && value.trim().equalsIgnoreCase("none");
    }

    public record Task(String id, String title, String description, String status, String author, String assignee) { }
}
