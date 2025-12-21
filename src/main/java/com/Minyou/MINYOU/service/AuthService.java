package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.*;
import com.Minyou.MINYOU.entity.User;
import com.Minyou.MINYOU.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * 이메일 인증 코드 전송
     */
    public void sendEmailVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && Boolean.TRUE.equals(existingUser.getEmailVerified())) {
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        // 6자리 랜덤 인증 코드 생성
        String verificationCode = String.format("%06d", (int)(Math.random() * 1000000));

        if (existingUser == null) {
            // 임시 사용자 생성 (임시 비밀번호, 닉네임, 이름, 아이디 사용)
            String tempPassword = passwordEncoder.encode(UUID.randomUUID().toString());
            String tempNickname = "temp_" + System.currentTimeMillis();
            String tempName = "temp";
            String tempUsername = "temp_" + System.currentTimeMillis();
            User tempUser = User.builder()
                    .email(email)
                    .password(tempPassword) // 임시 비밀번호
                    .nickname(tempNickname) // 임시 닉네임
                    .name(tempName) // 임시 이름
                    .username(tempUsername) // 임시 아이디
                    .emailVerified(false)
                    .emailVerificationCode(verificationCode)
                    .emailVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10))
                    .build();
            userRepository.save(tempUser);
        } else {
            // 기존 임시 사용자 업데이트
            existingUser.setEmailVerificationCode(verificationCode);
            existingUser.setEmailVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(existingUser);
        }

        // 이메일 전송
        emailService.sendVerificationCode(email, verificationCode);
    }

    /**
     * 이메일 인증 코드 검증
     */
    public boolean verifyEmailCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("이메일을 찾을 수 없습니다."));

        if (user.getEmailVerificationCode() == null || !user.getEmailVerificationCode().equals(code)) {
            throw new RuntimeException("인증 코드가 일치하지 않습니다.");
        }

        if (user.getEmailVerificationCodeExpiry() == null || 
            user.getEmailVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("인증 코드가 만료되었습니다.");
        }

        // 이메일 인증 완료 표시
        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationCodeExpiry(null);
        userRepository.save(user);

        return true;
    }

    public AuthResponse register(RegisterRequest request) {
        // 아이디 중복 확인
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }

        // 이메일 인증 여부 확인
        User tempUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("이메일 인증을 먼저 완료해주세요."));

        // 이미 완전히 가입된 사용자인지 확인
        if (Boolean.TRUE.equals(tempUser.getEmailVerified()) && 
            tempUser.getPassword() != null && 
            !tempUser.getPassword().startsWith("$2a$")) {
            // 임시 비밀번호가 아닌 경우 (이미 가입됨)
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        // 이메일 인증이 완료되지 않은 경우
        if (!Boolean.TRUE.equals(tempUser.getEmailVerified())) {
            throw new RuntimeException("이메일 인증이 완료되지 않았습니다. 먼저 이메일 인증을 완료해주세요.");
        }

        // 인증된 사용자 정보 업데이트
        tempUser.setPassword(passwordEncoder.encode(request.getPassword()));
        tempUser.setName(request.getName()); // 실제 이름 설정
        tempUser.setUsername(request.getUsername()); // 아이디 설정
        tempUser.setNickname(request.getUsername()); // 초기 닉네임은 아이디로 설정
        tempUser.setEmailVerified(true);
        tempUser.setEmailVerificationCode(null);
        tempUser.setEmailVerificationCodeExpiry(null);

        User user = userRepository.save(tempUser);

        String token = generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .user(UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .build())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .user(UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .build())
                .build();
    }

    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean checkUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 회원 정보 수정 (닉네임, 비밀번호)
     */
    public UserDto updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 닉네임 변경
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            user.setNickname(request.getNickname().trim());
        }

        // 비밀번호 변경
        if (request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty()) {
            // 현재 비밀번호 확인
            if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                throw new RuntimeException("현재 비밀번호를 입력해주세요.");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("현재 비밀번호가 올바르지 않습니다.");
            }
            // 새 비밀번호 유효성 검사 (8자 이상, 영문/숫자 포함)
            String newPassword = request.getNewPassword().trim();
            if (newPassword.length() < 8) {
                throw new RuntimeException("비밀번호는 8자 이상이어야 합니다.");
            }
            if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*[0-9].*")) {
                throw new RuntimeException("비밀번호는 영문과 숫자를 포함해야 합니다.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        user = userRepository.save(user);

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    /**
     * 이메일로 아이디 찾기 (이메일이 곧 아이디이므로 이메일 존재 여부 확인)
     */
    public Map<String, Object> findEmail(String email) {
        boolean exists = userRepository.existsByEmail(email);
        Map<String, Object> response = new HashMap<>();
        if (exists) {
            response.put("found", true);
            response.put("email", email);
            response.put("message", "등록된 이메일입니다.");
        } else {
            response.put("found", false);
            response.put("message", "등록되지 않은 이메일입니다.");
        }
        return response;
    }

    /**
     * 비밀번호 찾기 - 재설정 토큰 생성
     */
    public Map<String, Object> requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다."));

        // 재설정 토큰 생성 (30분 유효)
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", resetToken);
        response.put("message", "비밀번호 재설정 토큰이 생성되었습니다.");
        // 실제로는 이메일로 토큰을 전송해야 하지만, 여기서는 토큰을 반환
        return response;
    }

    /**
     * 비밀번호 재설정
     */
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 토큰입니다."));

        // 토큰 만료 확인
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("만료된 토큰입니다.");
        }

        // 새 비밀번호 유효성 검사
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("비밀번호를 입력해주세요.");
        }
        if (newPassword.length() < 8) {
            throw new RuntimeException("비밀번호는 8자 이상이어야 합니다.");
        }
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*[0-9].*")) {
            throw new RuntimeException("비밀번호는 영문과 숫자를 포함해야 합니다.");
        }

        // 비밀번호 변경 및 토큰 초기화
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private String generateToken(User user) {
        // 간단한 토큰 생성 (실제로는 JWT를 사용해야 함)
        return UUID.randomUUID().toString() + "_" + user.getId();
    }
}

