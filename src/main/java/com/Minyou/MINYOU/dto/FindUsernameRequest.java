package com.Minyou.MINYOU.dto;

import lombok.Data;

@Data
public class FindUsernameRequest {
    private String email;
    private String name; // 실제 이름
}
