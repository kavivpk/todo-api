package com.example.todo_api;

import com.example.todo_api.model.Task;
import com.example.todo_api.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:sqlite:target/task-api-http-test.db")
class TaskApiIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TaskRepository taskRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void clearTasks() {
        taskRepository.deleteAll();
    }

    @Test
    void supportsTaskCrudAndReturnsNotFoundForMissingIds() throws Exception {
        ApiResponse hello = get("/api/hello");
        assertEquals(HttpStatus.OK.value(), hello.statusCode());
        assertTrue(hello.body().contains("Todo API is running!"));

        ApiResponse emptyList = get("/api/tasks");
        assertEquals(HttpStatus.OK.value(), emptyList.statusCode());
        assertEquals(0, objectMapper.readTree(emptyList.body()).size());

        ApiResponse created = post("/api/tasks", """
                {"id":987654,"title":"Prepare demo","description":"Check the API","completed":false}
                """);
        assertEquals(HttpStatus.CREATED.value(), created.statusCode());
        JsonNode createdTask = objectMapper.readTree(created.body());
        long id = createdTask.get("id").asLong();
        assertNotEquals(987654L, id);

        ApiResponse fetched = get("/api/tasks/" + id);
        assertEquals(HttpStatus.OK.value(), fetched.statusCode());
        assertEquals("Prepare demo", objectMapper.readTree(fetched.body()).get("title").asText());

        ApiResponse updated = put("/api/tasks/" + id, """
                {"title":"Demo ready","description":"Endpoints verified","completed":true}
                """);
        assertEquals(HttpStatus.OK.value(), updated.statusCode());
        JsonNode updatedTask = objectMapper.readTree(updated.body());
        assertEquals("Demo ready", updatedTask.get("title").asText());
        assertEquals("Endpoints verified", updatedTask.get("description").asText());
        assertTrue(updatedTask.get("completed").asBoolean());

        ApiResponse deleted = delete("/api/tasks/" + id);
        assertEquals(HttpStatus.NO_CONTENT.value(), deleted.statusCode());
        assertTrue(deleted.body().isEmpty());
        assertEquals(HttpStatus.NOT_FOUND.value(), get("/api/tasks/" + id).statusCode());
    }

    @Test
    void rejectsBlankTitlesAndReturnsNotFoundForMissingTaskMutations() throws Exception {
        ApiResponse invalidCreate = post("/api/tasks", """
                {"title":"   ","description":"Invalid task","completed":false}
                """);
        assertEquals(HttpStatus.BAD_REQUEST.value(), invalidCreate.statusCode());
        assertErrorResponse(invalidCreate, 400, "Bad Request");

        ApiResponse invalidUpdate = put("/api/tasks/987654", """
                {"title":" ","description":"Invalid task","completed":false}
                """);
        assertEquals(HttpStatus.BAD_REQUEST.value(), invalidUpdate.statusCode());

        ApiResponse invalidFilter = get("/api/tasks?completed=not-a-boolean");
        assertEquals(HttpStatus.BAD_REQUEST.value(), invalidFilter.statusCode());
        assertErrorResponse(invalidFilter, 400, "Bad Request");

        ApiResponse missingGet = get("/api/tasks/987654");
        assertEquals(HttpStatus.NOT_FOUND.value(), missingGet.statusCode());
        assertErrorResponse(missingGet, 404, "Not Found");

        assertEquals(HttpStatus.NOT_FOUND.value(), put("/api/tasks/987654", """
                {"title":"Missing task","description":"","completed":false}
                """).statusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), delete("/api/tasks/987654").statusCode());
    }

    @Test
    void filtersAndSearchesTasksIncludingEmptyResults() throws Exception {
        taskRepository.save(new Task(null, "Learn SQL", "Practice queries", true));
        taskRepository.save(new Task(null, "Read Spring guide", "Study framework", false));

        JsonNode completed = objectMapper.readTree(get("/api/tasks?completed=true").body());
        assertEquals(1, completed.size());
        assertEquals("Learn SQL", completed.get(0).get("title").asText());

        JsonNode incomplete = objectMapper.readTree(get("/api/tasks?completed=false").body());
        assertEquals(1, incomplete.size());
        assertEquals("Read Spring guide", incomplete.get(0).get("title").asText());

        JsonNode matchingSearch = objectMapper.readTree(get("/api/tasks?search=sQl").body());
        assertEquals(1, matchingSearch.size());
        assertEquals("Learn SQL", matchingSearch.get(0).get("title").asText());

        JsonNode emptySearch = objectMapper.readTree(get("/api/tasks?search=no-match").body());
        assertEquals(0, emptySearch.size());
    }

    @Test
    void exposesOpenApiDocumentAndSwaggerUi() throws Exception {
        ApiResponse openApi = get("/v3/api-docs");
        assertEquals(HttpStatus.OK.value(), openApi.statusCode());
        assertNotNull(openApi.body());
        assertTrue(openApi.body().contains("/api/tasks"));

        ApiResponse swaggerUi = get("/swagger-ui/index.html");
        assertEquals(HttpStatus.OK.value(), swaggerUi.statusCode());
        assertNotNull(swaggerUi.body());
    }

    private ApiResponse get(String path) throws Exception {
        return send("GET", path, null);
    }

    private ApiResponse post(String path, String json) throws Exception {
        return send("POST", path, json);
    }

    private ApiResponse put(String path, String json) throws Exception {
        return send("PUT", path, json);
    }

    private ApiResponse delete(String path) throws Exception {
        return send("DELETE", path, null);
    }

    private ApiResponse send(String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Content-Type", "application/json")
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(response.statusCode(), response.body());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void assertErrorResponse(ApiResponse response, int status, String error) throws Exception {
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(status, body.get("status").asInt());
        assertEquals(error, body.get("error").asText());
        assertTrue(body.hasNonNull("message"));
    }

    private record ApiResponse(int statusCode, String body) {
    }
}
