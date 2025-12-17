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

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

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
                    log.info("STOMP user authenticated: {}", userId);
                } catch (RuntimeException e) {
                    log.error("STOMP connection authentication failed: {}", e.getMessage());
                    throw new SecurityException("Authentication failed: " + e.getMessage());
                }
            } else {
                log.warn("STOMP CONNECT without Authorization header or malformed token.");
            }
        }
        return message;
    }
}
