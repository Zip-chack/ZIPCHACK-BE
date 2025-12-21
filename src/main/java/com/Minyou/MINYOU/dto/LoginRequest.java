package com.Minyou.MINYOU.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email; // 이메일 또는 아이디 (username)
    private String password;
}

