package com.example.todo_api.controller;

import com.example.todo_api.model.Task;
import com.example.todo_api.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Create, read, update, delete, search, and filter tasks.")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(
            summary = "List and filter tasks",
            description = "Returns all tasks, optionally filtered by completion status and/or by a "
                    + "case-insensitive keyword in the task title. Returns an empty array when nothing matches.")
    @ApiResponse(
            responseCode = "200",
            description = "Matching tasks",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Task.class)),
                    examples = @ExampleObject(value = """
                            [
                              {
                                "id": 1,
                                "title": "Learn SQL",
                                "description": "Practice SQL queries",
                                "completed": true
                              }
                            ]
                            """)))
    public ResponseEntity<List<Task>> getTasks(
            @Parameter(description = "Filter by completion status.", example = "true")
            @RequestParam(required = false) Boolean completed,
            @Parameter(description = "Case-insensitive keyword to find in task titles.", example = "SQL")
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(taskService.getTasks(completed, search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task", description = "Finds one task by its generated ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Task.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "title": "Learn SQL",
                                      "description": "Practice SQL queries",
                                      "completed": true
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task ID does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiExceptionHandler.ApiError.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "Task not found with id: 99"
                                    }
                                    """)))
    })
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PostMapping
    @Operation(
            summary = "Create a task",
            description = "Creates a task in SQLite. The ID is generated by the server; any client-supplied ID is ignored.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Task created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Task.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "title": "Learn SQL",
                                      "description": "Practice SQL queries",
                                      "completed": false
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or blank title",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiExceptionHandler.ApiError.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "title: Title must not be blank"
                                    }
                                    """)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Task details. Do not provide an ID.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TaskRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "title": "Learn SQL",
                              "description": "Practice SQL queries",
                              "completed": false
                            }
                            """)))
    public ResponseEntity<Task> createTask(@Valid @RequestBody TaskRequest request) {
        Task createdTask = taskService.createTask(toTask(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTask.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdTask);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task", description = "Replaces the task title, description, and completion status.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Task.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "title": "Learn SQL",
                                      "description": "Practice SQL queries",
                                      "completed": true
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or blank title",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiExceptionHandler.ApiError.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "title: Title must not be blank"
                                    }
                                    """))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task ID does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiExceptionHandler.ApiError.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "Task not found with id: 99"
                                    }
                                    """)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Updated task details. The path ID identifies the task.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TaskRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "title": "Learn SQL",
                              "description": "Practice SQL queries",
                              "completed": true
                            }
                            """)))
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        Task updatedTask = taskService.updateTask(id, toTask(request));
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task", description = "Deletes the task with the specified ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task ID does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiExceptionHandler.ApiError.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "Task not found with id: 99"
                                    }
                                    """)))
    })
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    private Task toTask(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setCompleted(request.completed());
        return task;
    }

    public record TaskRequest(
            @Schema(description = "Required, non-blank task title.", example = "Learn SQL")
            @NotBlank(message = "Title must not be blank") String title,
            @Schema(description = "Optional task description.", example = "Practice SQL queries")
            String description,
            @Schema(description = "Whether the task is complete.", example = "false")
            boolean completed) {
    }
}
