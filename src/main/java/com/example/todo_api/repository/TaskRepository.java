package com.example.todo_api.repository;

import com.example.todo_api.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCompleted(boolean completed);

    List<Task> findByTitleContainingIgnoreCase(String title);

    List<Task> findByCompletedAndTitleContainingIgnoreCase(boolean completed, String title);
}
