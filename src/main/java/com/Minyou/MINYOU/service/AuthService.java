package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.*;
import com.Minyou.MINYOU.entity.User;
import com.Minyou.MINYOU.mapper.ChatMessageMapper;
import com.Minyou.MINYOU.mapper.ChatRoomMapper;
import com.Minyou.MINYOU.mapper.UserMapper;
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
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 이메일 인증 코드 전송
     */
    public void sendEmailVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        User existingUser = userMapper.findByEmail(email);
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
            userMapper.insert(tempUser);
        } else {
            // 기존 임시 사용자 업데이트
            existingUser.setEmailVerificationCode(verificationCode);
            existingUser.setEmailVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
            userMapper.update(existingUser);
        }

        // 이메일 전송 (실패해도 인증 코드는 저장됨)
        try {
            emailService.sendVerificationCode(email, verificationCode);
        } catch (Exception e) {
            // 이메일 전송 실패 시에도 인증 코드는 저장되어 있으므로, 개발 모드에서는 콘솔에 출력
            System.out.println("========================================");
            System.out.println("이메일 인증 코드 (개발용):");
            System.out.println("이메일: " + email);
            System.out.println("인증 코드: " + verificationCode);
            System.out.println("========================================");
            // 개발 모드에서는 예외를 던지지 않고 계속 진행
            // throw new RuntimeException("이메일 전송에 실패했습니다. 개발 모드에서는 콘솔을 확인하세요.");
        }
    }

    /**
     * 이메일 인증 코드 검증
     */
    public boolean verifyEmailCode(String email, String code) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("이메일을 찾을 수 없습니다.");
        }

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
        userMapper.update(user);

        return true;
    }

    public AuthResponse register(RegisterRequest request) {
        // 아이디 중복 확인
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }

        // 이메일 인증 여부 확인
        User tempUser = userMapper.findByEmail(request.getEmail());
        if (tempUser == null) {
            throw new RuntimeException("이메일 인증을 먼저 완료해주세요.");
        }

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

        userMapper.update(tempUser);
        User user = tempUser;

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
        // 이메일 또는 아이디로 사용자 찾기
        User user = userMapper.findByEmail(request.getEmail());
        if (user == null) {
            user = userMapper.findByUsername(request.getEmail());
        }
        
        if (user == null) {
            throw new RuntimeException("아이디(이메일) 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("아이디(이메일) 또는 비밀번호가 올바르지 않습니다.");
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
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    public boolean checkEmailExists(String email) {
        return userMapper.existsByEmail(email);
    }

    public boolean checkUsernameExists(String username) {
        return userMapper.existsByUsername(username);
    }

    /**
     * 회원 정보 수정 (닉네임, 비밀번호)
     */
    public UserDto updateUser(Long userId, UpdateUserRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

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

        userMapper.update(user);

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    /**
     * 아이디 찾기 (이메일 + 이름으로 확인)
     * 일치하면 아이디만 반환
     */
    public Map<String, Object> findUsername(String email, String name) {
        User user = userMapper.findByEmail(email);

        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("found", false);
            response.put("message", "등록되지 않은 이메일입니다.");
            return response;
        }

        // 이름 확인
        if (!name.equals(user.getName())) {
            response.put("found", false);
            response.put("message", "이메일과 이름이 일치하지 않습니다.");
            return response;
        }

        // 일치하는 경우: 아이디만 반환
        response.put("found", true);
        response.put("username", user.getUsername());
        response.put("message", "아이디를 찾았습니다.");
        
        return response;
    }

    /**
     * 비밀번호 찾기 - 인증 코드 전송
     */
    public Map<String, Object> requestPasswordReset(String email) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("등록되지 않은 이메일입니다.");
        }

        // 6자리 랜덤 인증 코드 생성
        String verificationCode = String.format("%06d", (int)(Math.random() * 1000000));

        // 인증 코드 저장 (10분 유효)
        user.setEmailVerificationCode(verificationCode);
        user.setEmailVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
        userMapper.update(user);

        // 이메일로 인증 코드 전송 (실패해도 인증 코드는 저장됨)
        try {
            emailService.sendPasswordResetCode(email, verificationCode);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "인증 코드가 전송되었습니다. 이메일을 확인해주세요.");
            return response;
        } catch (Exception e) {
            // 이메일 전송 실패 시에도 인증 코드는 저장되어 있으므로, 개발 모드에서는 콘솔에 출력
            System.out.println("========================================");
            System.out.println("비밀번호 재설정 인증 코드 (개발용):");
            System.out.println("이메일: " + email);
            System.out.println("인증 코드: " + verificationCode);
            System.out.println("========================================");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "인증 코드가 생성되었습니다. (이메일 전송 실패 - 개발 모드에서는 콘솔을 확인하세요)");
            response.put("devCode", verificationCode); // 개발 모드에서만 사용
            return response;
        }
    }

    /**
     * 비밀번호 찾기용 인증 코드 검증
     */
    public Map<String, Object> verifyPasswordResetCode(String email, String code) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("등록되지 않은 이메일입니다.");
        }

        Map<String, Object> response = new HashMap<>();

        // 인증 코드 확인
        if (user.getEmailVerificationCode() == null || !user.getEmailVerificationCode().equals(code)) {
            response.put("success", false);
            response.put("message", "인증 코드가 일치하지 않습니다.");
            return response;
        }

        // 인증 코드 만료 확인
        if (user.getEmailVerificationCodeExpiry() == null || 
            user.getEmailVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            response.put("success", false);
            response.put("message", "만료된 인증 코드입니다.");
            return response;
        }

        response.put("success", true);
        response.put("message", "인증이 완료되었습니다.");
        return response;
    }

    /**
     * 비밀번호 재설정 (인증 코드 검증 후)
     */
    public void resetPassword(String email, String code, String newPassword) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("등록되지 않은 이메일입니다.");
        }

        // 인증 코드 확인
        if (user.getEmailVerificationCode() == null || !user.getEmailVerificationCode().equals(code)) {
            throw new RuntimeException("인증 코드가 일치하지 않습니다.");
        }

        // 인증 코드 만료 확인
        if (user.getEmailVerificationCodeExpiry() == null || 
            user.getEmailVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("만료된 인증 코드입니다.");
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

        // 비밀번호 변경 및 인증 코드 초기화
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setEmailVerificationCode(null);
        user.setEmailVerificationCodeExpiry(null);
        userMapper.update(user);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 1. 사용자가 참여한 채팅방 찾기
        var chatRooms = chatRoomMapper.findByOwnerIdOrBuyerId(userId, userId);
        
        // 2. 각 채팅방의 메시지 삭제
        for (var room : chatRooms) {
            chatMessageMapper.deleteByChatRoomId(room.getId());
        }
        
        // 3. 사용자가 참여한 채팅방 삭제
        for (var room : chatRooms) {
            chatRoomMapper.delete(room.getId());
        }
        
        // 4. 찜한 매물 관계 삭제는 FavoriteMapper로 처리
        // (User 엔티티의 getFavoriteListings()는 JPA 관계이므로 MyBatis에서는 직접 처리)
        
        // 5. 사용자 삭제
        userMapper.delete(userId);
    }

    private String generateToken(User user) {
        // 간단한 토큰 생성 (실제로는 JWT를 사용해야 함)
        return UUID.randomUUID().toString() + "_" + user.getId();
    }
}

