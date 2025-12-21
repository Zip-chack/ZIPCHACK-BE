package com.Minyou.MINYOU.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String name; // 실제 이름
    private String username; // 아이디
}

