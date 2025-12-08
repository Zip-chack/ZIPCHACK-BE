package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.AuthResponse;
import com.Minyou.MINYOU.dto.LoginRequest;
import com.Minyou.MINYOU.dto.RegisterRequest;
import com.Minyou.MINYOU.dto.UserDto;
import com.Minyou.MINYOU.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            UserDto user = authService.getCurrentUser(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Long extractUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token");
        }
        String tokenValue = token.substring(7);
        String[] parts = tokenValue.split("_");
        if (parts.length < 2) {
            throw new RuntimeException("Invalid token format");
        }
        return Long.parseLong(parts[parts.length - 1]);
    }
}

