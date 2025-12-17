package com.Minyou.MINYOU.security;

import org.springframework.stereotype.Component;

@Component
public class TokenUtil {

    public Long extractUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token format or missing token.");
        }
        String tokenValue = token.substring(7);
        String[] parts = tokenValue.split("_");
        if (parts.length > 0) {
            try {
                return Long.parseLong(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid user ID in token.", e);
            }
        }
        throw new RuntimeException("Invalid token format.");
    }
}
