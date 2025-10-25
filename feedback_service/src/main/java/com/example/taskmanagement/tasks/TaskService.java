package com.example.taskmanagement.tasks;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse createTask(TaskRequest request, String authorEmail) {
        String normalizedAuthor = authorEmail.toLowerCase(Locale.ROOT);
        Task task = new Task(
                request.title().trim(),
                request.description().trim(),
                normalizedAuthor,
                TaskStatus.CREATED
        );
        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    public List<TaskResponse> getTasks(String author) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        List<Task> tasks;
        if (author == null) {
            tasks = taskRepository.findAll(sort);
        } else {
            String normalizedAuthor = author.trim().toLowerCase(Locale.ROOT);
            tasks = taskRepository.findAllByAuthor(normalizedAuthor, sort);
        }
        return tasks.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TaskResponse mapToResponse(Task task) {
        return new TaskResponse(
                task.getId() != null ? String.valueOf(task.getId()) : null,
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getAuthor()
        );
    }
}
