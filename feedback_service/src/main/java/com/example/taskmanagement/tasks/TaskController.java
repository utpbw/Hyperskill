package com.example.taskmanagement.tasks;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Locale;

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
        String authorEmail = authentication.getName().toLowerCase(Locale.ROOT);
        return taskService.createTask(request, authorEmail);
    }

    @GetMapping
    public List<TaskResponse> getTasks(@RequestParam(value = "author", required = false) String author) {
        return taskService.getTasks(author);
    }
}
