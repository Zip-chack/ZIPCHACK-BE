package com.Minyou.MINYOU.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "MINYOU API Server is running");
        response.put("status", "OK");
        response.put("version", "1.0.0");
        response.put("endpoints", Map.of(
                "auth", "/api/auth",
                "listings", "/api/listings",
                "buildings", "/api/buildings",
                "reviews", "/api/reviews"
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }
}