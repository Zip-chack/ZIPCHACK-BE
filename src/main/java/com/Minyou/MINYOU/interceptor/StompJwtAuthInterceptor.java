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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet; // More efficient for concurrent removals than ConcurrentHashMap.newKeySet()

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtAuthInterceptor implements ChannelInterceptor {

    private final TokenUtil tokenUtil;

    // userId -> Set of sessionIds
    private final Map<Long, ConcurrentSkipListSet<String>> activeSessions = new ConcurrentHashMap<>();

    public Map<Long, ConcurrentSkipListSet<String>> getActiveSessions() {
        return activeSessions;
    }

    /**
     * STOMP CONNECT/DISCONNECT 요청 시 JWT를 검증하여 WebSocket 사용자 인증 및 세션 관리를 처리한다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        String sessionId = accessor.getSessionId();

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwtToken = accessor.getFirstNativeHeader("Authorization");

            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                try {
                    Long userId = tokenUtil.extractUserIdFromToken(jwtToken);
                    UserPrincipal principal = new UserPrincipal(userId, String.valueOf(userId));
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    accessor.setUser(authentication);
                    log.info("STOMP user authenticated: userId={}, sessionId={}", userId, sessionId);
                    
                    // Add session to active sessions map
                    activeSessions.computeIfAbsent(userId, k -> new ConcurrentSkipListSet<>()).add(sessionId);

                } catch (RuntimeException e) {
                    log.error("STOMP connection authentication failed: {}", e.getMessage());
                    throw new SecurityException("Authentication failed: " + e.getMessage());
                }
            } else {
                log.warn("STOMP CONNECT without Authorization header or malformed token. SessionId: {}", sessionId);
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Authentication authentication = (Authentication) accessor.getUser();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
                log.info("STOMP user disconnected: userId={}, sessionId={}", userId, sessionId);
                
                // Remove session from active sessions map
                ConcurrentSkipListSet<String> userSessions = activeSessions.get(userId);
                if (userSessions != null) {
                    userSessions.remove(sessionId);
                    if (userSessions.isEmpty()) {
                        activeSessions.remove(userId);
                    }
                }
            } else {
                log.warn("STOMP DISCONNECT for unauthenticated or unknown user. SessionId: {}", sessionId);
            }
        }
        return message;
    }
}
