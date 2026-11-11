package com.example.todo_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/hello")
    @Operation(summary = "Check API status", description = "Returns a message confirming the To-Do API is running.")
    public String hello() {
        return "Todo API is running!";
    }
}