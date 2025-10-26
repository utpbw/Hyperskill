package com.example.accounts.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TaskService {

    private static final String CREATED_STATUS = "CREATED";
    private static final Set<String> VALID_STATUSES = Set.of(
            CREATED_STATUS,
            "IN_PROGRESS",
            "COMPLETED"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final TaskRepository taskRepository;
    private final AccountRepository accountRepository;
    private final TaskCommentRepository taskCommentRepository;

    public TaskService(TaskRepository taskRepository,
                       AccountRepository accountRepository,
                       TaskCommentRepository taskCommentRepository) {
        this.taskRepository = taskRepository;
        this.accountRepository = accountRepository;
        this.taskCommentRepository = taskCommentRepository;
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
    public List<Task> getAllTasks(String authorEmail, String assigneeEmail) {
        String normalizedAuthor = normalizeFilterValue(authorEmail);
        String normalizedAssignee = normalizeFilterValue(assigneeEmail);

        List<TaskEntity> entities;
        if (normalizedAuthor != null && normalizedAssignee != null) {
            entities = taskRepository.findAllByAuthorEmailAndAssigneeEmailOrderByIdDesc(
                    normalizedAuthor,
                    normalizedAssignee
            );
        } else if (normalizedAuthor != null) {
            entities = taskRepository.findAllByAuthorEmailOrderByIdDesc(normalizedAuthor);
        } else if (normalizedAssignee != null) {
            entities = taskRepository.findAllByAssigneeEmailOrderByIdDesc(normalizedAssignee);
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

    @Transactional
    public Task updateStatus(long taskId, String requestedStatus, String actingUserEmail) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        String trimmedStatus = requestedStatus == null ? null : requestedStatus.trim();
        if (!StringUtils.hasText(trimmedStatus) || !VALID_STATUSES.contains(trimmedStatus)) {
            throw new ResponseStatusException(BAD_REQUEST);
        }

        String normalizedActor = normalizeEmail(actingUserEmail);
        String assignee = task.getAssigneeEmail();
        if (!task.getAuthorEmail().equals(normalizedActor) && (assignee == null || !assignee.equals(normalizedActor))) {
            throw new ResponseStatusException(FORBIDDEN);
        }

        task.setStatus(trimmedStatus);
        TaskEntity saved = taskRepository.save(task);
        return toTask(saved);
    }

    @Transactional
    public TaskComment addComment(long taskId, String commentText, String commentingUserEmail) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        String normalizedAuthor = normalizeEmail(commentingUserEmail);
        String sanitizedText = sanitizeCommentText(commentText);

        TaskCommentEntity entity = new TaskCommentEntity();
        entity.setTask(task);
        entity.setAuthorEmail(normalizedAuthor);
        entity.setText(sanitizedText);

        TaskCommentEntity saved = taskCommentRepository.save(entity);
        return toComment(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskComment> getCommentsForTask(long taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        return taskCommentRepository.findAllByTaskOrderByIdDesc(task)
                .stream()
                .map(this::toComment)
                .collect(Collectors.toList());
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

    private String normalizeFilterValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }

        return normalizeEmail(trimmed);
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

    private String sanitizeCommentText(String text) {
        if (text == null) {
            throw new ResponseStatusException(BAD_REQUEST);
        }

        String trimmed = text.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new ResponseStatusException(BAD_REQUEST);
        }

        return trimmed;
    }

    public record Task(String id, String title, String description, String status, String author, String assignee) { }

    public record TaskComment(String id, String taskId, String text, String author) { }

    private TaskComment toComment(TaskCommentEntity entity) {
        return new TaskComment(
                String.valueOf(entity.getId()),
                String.valueOf(entity.getTask().getId()),
                entity.getText(),
                entity.getAuthorEmail()
        );
    }
}
