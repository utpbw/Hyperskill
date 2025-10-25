package com.example.taskmanagement.tasks;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping(path = "/api/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String authorEmail = authentication.getName();
        return taskService.createTask(request, authorEmail);
    }

    @GetMapping
    public List<TaskResponse> getTasks(@RequestParam(value = "author", required = false) String author,
                                       @RequestParam(value = "assignee", required = false) String assignee) {
        return taskService.getTasks(author, assignee);
    }

    @PutMapping(path = "/{taskId}/assign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskResponse assignTask(@PathVariable("taskId") long taskId,
                                   @Valid @RequestBody TaskAssignmentRequest request,
                                   Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return taskService.assignTask(taskId, request, authentication.getName());
    }

    @PutMapping(path = "/{taskId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskResponse updateStatus(@PathVariable("taskId") long taskId,
                                     @Valid @RequestBody TaskStatusUpdateRequest request,
                                     Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return taskService.updateStatus(taskId, request, authentication.getName());
    }
}
