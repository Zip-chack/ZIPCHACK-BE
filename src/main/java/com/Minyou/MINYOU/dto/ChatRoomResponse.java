package com.Minyou.MINYOU.dto;

import com.Minyou.MINYOU.entity.ChatRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {
    private Long roomId;
    private Long targetUserId;
    private String targetUserNickname;
    private ChatRoomStatus status;
}
