package com.Minyou.MINYOU.interceptor;

import com.Minyou.MINYOU.security.TokenUtil;
import com.Minyou.MINYOU.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtAuthInterceptor implements ChannelInterceptor {

    private final TokenUtil tokenUtil;

    /**
     * STOMP CONNECT 요청 시 JWT를 검증하여 WebSocket 사용자 인증을 처리한다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // STOMP 연결 요청일 경우에만 인증 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwtToken = accessor.getFirstNativeHeader("Authorization");

            // Authorization 헤더에서 Bearer 토큰 추출
            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                try {
                    // JWT에서 사용자 ID 추출
                    Long userId = tokenUtil.extractUserIdFromToken(jwtToken);

                    // WebSocket 세션에 저장될 사용자 Principal 생성
                    UserPrincipal principal = new UserPrincipal(userId, String.valueOf(userId));
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    // STOMP 세션에 인증 정보 저장
                    accessor.setUser(authentication);
                    log.info("STOMP user authenticated: {}", userId);
                } catch (RuntimeException e) {
                    // 토큰 검증 실패 시 연결 거부
                    log.error("STOMP connection authentication failed: {}", e.getMessage());
                    throw new SecurityException("Authentication failed: " + e.getMessage());
                }
            } else {
                // 토큰이 없거나 형식이 잘못된 경우
                log.warn("STOMP CONNECT without Authorization header or malformed token.");
            }
        }
        return message;
    }
}
