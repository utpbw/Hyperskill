package com.example.taskmanagement.tasks;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(task -> new TaskResponse(task.getId(), task.getTitle(), task.getDescription()))
                .toList();
    }
}
