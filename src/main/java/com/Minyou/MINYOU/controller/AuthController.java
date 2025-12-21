package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.AuthResponse;
import com.Minyou.MINYOU.dto.LoginRequest;
import com.Minyou.MINYOU.dto.RegisterRequest;
import com.Minyou.MINYOU.dto.UpdateUserRequest;
import com.Minyou.MINYOU.dto.UserDto;
import com.Minyou.MINYOU.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        // 예외를 GlobalExceptionHandler가 처리하도록 throw
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // 예외를 GlobalExceptionHandler가 처리하도록 throw
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
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

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean exists = authService.checkEmailExists(email);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("message", exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateUser(
            @RequestBody UpdateUserRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            UserDto updatedUser = authService.updateUser(userId, request);
            return ResponseEntity.ok(updatedUser);
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

