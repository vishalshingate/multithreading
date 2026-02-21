package com.todo.controller;

import com.todo.model.Task;
import com.todo.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @PutMapping("/tasks/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Task taskUpdate) {
        if (taskUpdate.getDescription() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Task description is required");
            error.put("status", 400);
            return ResponseEntity.status(400).body(error);
        }

        Optional<Task> existingTaskOpt = taskRepository.findById(id);
        if (existingTaskOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Cannot find task with given id");
            error.put("status", 404);
            return ResponseEntity.status(404).body(error);
        }

        Task existingTask = existingTaskOpt.get();
        existingTask.setDescription(taskUpdate.getDescription());
        if (taskUpdate.getPriority() != null) {
            existingTask.setPriority(taskUpdate.getPriority());
        }

        taskRepository.save(existingTask);

        return ResponseEntity.ok(existingTask); // Or whatever success response is needed, usually the updated task
    }
}

