package com.example.accounts.api;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TaskService {

    private final AtomicLong idGenerator = new AtomicLong();
    private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

    public Task createTask(TaskRequest request, String authorEmail) {
        String id = Long.toString(idGenerator.incrementAndGet());
        String normalizedAuthor = authorEmail == null ? null : authorEmail.trim().toLowerCase(Locale.ROOT);
        Task task = new Task(
                id,
                request.title().trim(),
                request.description().trim(),
                "CREATED",
                normalizedAuthor
        );
        tasks.put(id, task);
        return task;
    }

    public Collection<Task> getAllTasks() {
        return List.copyOf(tasks.values());
    }

    public record Task(String id, String title, String description, String status, String author) { }
}
