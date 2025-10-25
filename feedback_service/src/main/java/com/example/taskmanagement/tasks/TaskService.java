package com.example.taskmanagement.tasks;

import com.example.taskmanagement.auth.AccountUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TaskService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final TaskRepository taskRepository;
    private final AccountUserRepository accountUserRepository;
    private final TaskCommentRepository taskCommentRepository;

    public TaskService(TaskRepository taskRepository,
                       AccountUserRepository accountUserRepository,
                       TaskCommentRepository taskCommentRepository) {
        this.taskRepository = taskRepository;
        this.accountUserRepository = accountUserRepository;
        this.taskCommentRepository = taskCommentRepository;
    }

    public TaskResponse createTask(TaskRequest request, String authorEmail) {
        if (authorEmail == null || authorEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user required");
        }

        String normalizedAuthor = normalizeEmail(authorEmail);
        Task task = new Task(
                request.title().trim(),
                request.description().trim(),
                normalizedAuthor,
                TaskStatus.CREATED,
                null
        );
        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    public TaskResponse assignTask(long taskId, TaskAssignmentRequest request, String requesterEmail) {
        Task task = findTask(taskId);
        String normalizedRequester = normalizeEmail(requireAuthenticatedEmail(requesterEmail));
        if (!task.getAuthor().equals(normalizedRequester)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can assign the task");
        }

        String assigneeInput = request.assignee();
        if (assigneeInput == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must be provided");
        }
        String trimmedAssignee = assigneeInput.trim();
        if (trimmedAssignee.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must not be blank");
        }

        if (trimmedAssignee.equalsIgnoreCase("none")) {
            task.setAssignee(null);
        } else {
            if (!EMAIL_PATTERN.matcher(trimmedAssignee).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee email must be valid");
            }
            String normalizedAssignee = trimmedAssignee.toLowerCase(Locale.ROOT);
            if (!accountUserRepository.existsByEmailIgnoreCase(normalizedAssignee)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found");
            }
            task.setAssignee(normalizedAssignee);
        }

        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    public TaskResponse updateStatus(long taskId, TaskStatusUpdateRequest request, String requesterEmail) {
        Task task = findTask(taskId);
        String normalizedRequester = normalizeEmail(requireAuthenticatedEmail(requesterEmail));

        boolean isAuthor = task.getAuthor().equals(normalizedRequester);
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().equals(normalizedRequester);
        if (!isAuthor && !isAssignee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author or assignee can change status");
        }

        task.setStatus(request.status());
        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    public TaskCommentResponse addComment(long taskId, TaskCommentRequest request, String commenterEmail) {
        Task task = findTask(taskId);
        String normalizedCommenter = normalizeEmail(requireAuthenticatedEmail(commenterEmail));

        String text = request.text();
        if (text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment text must be provided");
        }
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment text must not be blank");
        }

        TaskComment comment = new TaskComment(task, normalizedCommenter, trimmedText);
        TaskComment saved = taskCommentRepository.save(comment);
        return mapToCommentResponse(saved);
    }

    public List<TaskCommentResponse> getComments(long taskId) {
        Task task = findTask(taskId);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        return taskCommentRepository.findAllByTask(task, sort)
                .stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    public List<TaskResponse> getTasks(String author, String assignee) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        String normalizedAuthor = normalizeOptionalEmail(author);
        String normalizedAssignee = normalizeOptionalEmail(assignee);

        boolean filterAssigneeNone = normalizedAssignee != null && normalizedAssignee.equals("none");

        List<Task> tasks;
        if (normalizedAuthor != null && normalizedAssignee != null) {
            if (filterAssigneeNone) {
                tasks = taskRepository.findAllByAuthorAndAssigneeIsNull(normalizedAuthor, sort);
            } else {
                tasks = taskRepository.findAllByAuthorAndAssignee(normalizedAuthor, normalizedAssignee, sort);
            }
        } else if (normalizedAuthor != null) {
            tasks = taskRepository.findAllByAuthor(normalizedAuthor, sort);
        } else if (normalizedAssignee != null) {
            if (filterAssigneeNone) {
                tasks = taskRepository.findAllByAssigneeIsNull(sort);
            } else {
                tasks = taskRepository.findAllByAssignee(normalizedAssignee, sort);
            }
        } else {
            tasks = taskRepository.findAll(sort);
        }

        return tasks.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Task findTask(long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private String requireAuthenticatedEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user required");
        }
        return email;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private TaskResponse mapToResponse(Task task) {
        return new TaskResponse(
                task.getId() != null ? String.valueOf(task.getId()) : null,
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getAuthor(),
                task.getAssignee() == null ? "none" : task.getAssignee()
        );
    }

    private TaskCommentResponse mapToCommentResponse(TaskComment comment) {
        return new TaskCommentResponse(
                comment.getId() != null ? String.valueOf(comment.getId()) : null,
                comment.getTask() != null && comment.getTask().getId() != null
                        ? String.valueOf(comment.getTask().getId())
                        : null,
                comment.getText(),
                comment.getAuthor()
        );
    }
}
