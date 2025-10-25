package com.example.taskmanagement.tasks;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskDataInitializer implements CommandLineRunner {

    private final TaskRepository taskRepository;

    public TaskDataInitializer(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(String... args) {
        if (taskRepository.count() == 0) {
            taskRepository.saveAll(List.of(
                    new Task("Plan sprint", "Outline tasks and priorities for the next sprint.", "system@taskmanagement.local", TaskStatus.CREATED),
                    new Task("Review pull requests", "Go through pending PRs and provide feedback.", "system@taskmanagement.local", TaskStatus.CREATED),
                    new Task("Prepare release notes", "Compile updates for the upcoming release.", "system@taskmanagement.local", TaskStatus.CREATED)
            ));
        }
    }
}
