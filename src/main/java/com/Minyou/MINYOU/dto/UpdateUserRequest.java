package com.Minyou.MINYOU.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String nickname;
    private String currentPassword;
    private String newPassword;
}
