package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.AuthResponse;
import com.Minyou.MINYOU.dto.FindEmailRequest;
import com.Minyou.MINYOU.dto.FindPasswordRequest;
import com.Minyou.MINYOU.dto.LoginRequest;
import com.Minyou.MINYOU.dto.RegisterRequest;
import com.Minyou.MINYOU.dto.ResetPasswordRequest;
import com.Minyou.MINYOU.dto.UpdateUserRequest;
import com.Minyou.MINYOU.dto.UserDto;
import com.Minyou.MINYOU.dto.VerifyEmailRequest;
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

    /**
     * 이메일 인증 코드 전송
     */
    @PostMapping("/send-verification-code")
    public ResponseEntity<Map<String, Object>> sendVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "이메일을 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            authService.sendEmailVerificationCode(email.trim());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "인증 코드가 전송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 이메일 인증 코드 검증
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody VerifyEmailRequest request) {
        try {
            authService.verifyEmailCode(request.getEmail(), request.getCode());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "이메일 인증이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

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

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        boolean exists = authService.checkUsernameExists(username);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("message", exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");
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

    /**
     * 아이디 찾기 (이메일 확인)
     */
    @PostMapping("/find-email")
    public ResponseEntity<Map<String, Object>> findEmail(@RequestBody FindEmailRequest request) {
        try {
            Map<String, Object> response = authService.findEmail(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("found", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 비밀번호 찾기 - 재설정 토큰 요청
     */
    @PostMapping("/find-password")
    public ResponseEntity<Map<String, Object>> findPassword(@RequestBody FindPasswordRequest request) {
        try {
            Map<String, Object> response = authService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 비밀번호 재설정
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "비밀번호가 성공적으로 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
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

