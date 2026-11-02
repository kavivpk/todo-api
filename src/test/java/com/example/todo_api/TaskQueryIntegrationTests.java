package com.example.todo_api;

import com.example.todo_api.model.Task;
import com.example.todo_api.repository.TaskRepository;
import com.example.todo_api.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/task-query-test.db")
class TaskQueryIntegrationTests {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        taskRepository.save(new Task(null, "Learn SQL", "Practice SQL queries", true));
        taskRepository.save(new Task(null, "Read Spring guide", "Study Spring Boot", false));
    }

    @Test
    void filtersTasksByCompletionStatus() {
        assertEquals(1, taskService.getTasks(true, null).size());
        assertEquals("Learn SQL", taskService.getTasks(true, null).get(0).getTitle());

        assertEquals(1, taskService.getTasks(false, null).size());
        assertEquals("Read Spring guide", taskService.getTasks(false, null).get(0).getTitle());
    }

    @Test
    void searchesTitlesIgnoringCaseAndReturnsEmptyResultsWhenNoTitleMatches() {
        assertEquals(1, taskService.getTasks(null, "sQl").size());
        assertEquals("Learn SQL", taskService.getTasks(null, "sQl").get(0).getTitle());

        assertEquals(0, taskService.getTasks(null, "no matching title").size());
    }
}
