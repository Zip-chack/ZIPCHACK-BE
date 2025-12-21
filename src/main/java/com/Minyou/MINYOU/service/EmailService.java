package com.Minyou.MINYOU.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@zipchack.com}")
    private String fromEmail;

    @Value("${spring.mail.from:${spring.mail.username}}")
    private String fromAddress;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * 이메일 인증 코드 전송
     */
    public void sendVerificationCode(String toEmail, String verificationCode) {
        if (!emailEnabled) {
            log.info("[개발 모드] 이메일 전송 비활성화 - {}에게 인증 코드 {} 전송 (실제 전송 안 함)", toEmail, verificationCode);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // 네이버 메일의 경우 발신자 주소가 인증된 메일 주소여야 함
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("[ZIP-Chack] 이메일 인증 코드");
            message.setText(
                "안녕하세요, ZIP-Chack입니다.\n\n" +
                "이메일 인증 코드는 다음과 같습니다:\n\n" +
                verificationCode + "\n\n" +
                "이 코드는 10분간 유효합니다.\n" +
                "본인이 요청한 것이 아니라면 무시하셔도 됩니다.\n\n" +
                "감사합니다."
            );

            mailSender.send(message);
            log.info("이메일 인증 코드 전송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }

    /**
     * 아이디 찾기 시 아이디만 전송
     */
    public void sendUsername(String toEmail, String username) {
        if (!emailEnabled) {
            log.info("[개발 모드] 이메일 전송 비활성화 - {}에게 아이디: {} 전송 (실제 전송 안 함)", 
                    toEmail, username);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("[ZIP-Chack] 아이디 찾기 결과");
            message.setText(
                "안녕하세요, ZIP-Chack입니다.\n\n" +
                "요청하신 아이디 찾기 결과입니다.\n\n" +
                "아이디: " + username + "\n\n" +
                "본인이 요청한 것이 아니라면 고객센터로 문의해주세요.\n\n" +
                "감사합니다."
            );

            mailSender.send(message);
            log.info("아이디 전송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }

    /**
     * 비밀번호 찾기용 인증 코드 전송
     */
    public void sendPasswordResetCode(String toEmail, String verificationCode) {
        if (!emailEnabled) {
            log.info("[개발 모드] 이메일 전송 비활성화 - {}에게 비밀번호 재설정 인증 코드 {} 전송 (실제 전송 안 함)", 
                    toEmail, verificationCode);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("[ZIP-Chack] 비밀번호 재설정 인증 코드");
            message.setText(
                "안녕하세요, ZIP-Chack입니다.\n\n" +
                "비밀번호 재설정을 위한 인증 코드는 다음과 같습니다:\n\n" +
                verificationCode + "\n\n" +
                "이 코드는 10분간 유효합니다.\n" +
                "본인이 요청한 것이 아니라면 무시하셔도 됩니다.\n\n" +
                "감사합니다."
            );

            mailSender.send(message);
            log.info("비밀번호 재설정 인증 코드 전송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.");
        }
    }
}
