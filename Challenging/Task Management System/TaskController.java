package com.example.accounts.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskService.Task>> listTasks(
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "assignee", required = false) String assignee) {
        return ResponseEntity.ok(taskService.getAllTasks(author, assignee));
    }

    @PostMapping
    public ResponseEntity<TaskService.Task> createTask(@Valid @RequestBody TaskRequest request,
                                                       Authentication authentication) {
        String author = Objects.requireNonNull(authentication, "authentication").getName();
        TaskService.Task task = taskService.createTask(request, author);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{taskId}/assign")
    public ResponseEntity<TaskService.Task> assignTask(@PathVariable("taskId") long taskId,
                                                       @Valid @RequestBody TaskAssignmentRequest request,
                                                       Authentication authentication) {
        String actingUser = Objects.requireNonNull(authentication, "authentication").getName();
        TaskService.Task task = taskService.assignTask(taskId, request.assignee(), actingUser);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{taskId}/status")
    public ResponseEntity<TaskService.Task> updateStatus(@PathVariable("taskId") long taskId,
                                                         @Valid @RequestBody TaskStatusRequest request,
                                                         Authentication authentication) {
        String actingUser = Objects.requireNonNull(authentication, "authentication").getName();
        TaskService.Task task = taskService.updateStatus(taskId, request.status(), actingUser);
        return ResponseEntity.ok(task);
    }

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<TaskService.TaskComment> addComment(@PathVariable("taskId") long taskId,
                                                              @Valid @RequestBody TaskCommentRequest request,
                                                              Authentication authentication) {
        String commentingUser = Objects.requireNonNull(authentication, "authentication").getName();
        TaskService.TaskComment comment = taskService.addComment(taskId, request.text(), commentingUser);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<TaskService.TaskComment>> getComments(@PathVariable("taskId") long taskId,
                                                                     Authentication authentication) {
        Objects.requireNonNull(authentication, "authentication");
        List<TaskService.TaskComment> comments = taskService.getCommentsForTask(taskId);
        return ResponseEntity.ok(comments);
    }
}
