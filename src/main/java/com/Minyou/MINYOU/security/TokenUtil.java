package com.Minyou.MINYOU.security;

import org.springframework.stereotype.Component;

@Component
public class TokenUtil {

    /**
     * Authorization 헤더의 Bearer 토큰에서 사용자 ID를 추출한다.
     */
    public Long extractUserIdFromToken(String token) {
        // 토큰이 없거나 Bearer 형식이 아닌 경우 예외 발생
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token format or missing token.");
        }
        // "Bearer " 제거 후 실제 토큰 값 추출
        String tokenValue = token.substring(7);

        // 토큰을 "_" 기준으로 분리하여 마지막 값을 사용자 ID로 사용
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
