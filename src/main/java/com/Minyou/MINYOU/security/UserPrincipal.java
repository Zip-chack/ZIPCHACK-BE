package com.Minyou.MINYOU.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.Principal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements Principal {
    private Long id;
    private String name;

    /**
     * Spring Security 및 WebSocket에서 사용자 식별에 사용되는 이름을 반환한다.
     */
    @Override
    public String getName() {
        return name;
    }
}