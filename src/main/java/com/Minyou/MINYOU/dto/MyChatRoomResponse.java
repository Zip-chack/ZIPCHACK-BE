package com.Minyou.MINYOU.dto;

import com.Minyou.MINYOU.entity.ChatRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyChatRoomResponse {
    private Long roomId;
    private Long targetUserId;
    private String targetUserNickname;
    private String lastMessage;
    private ChatRoomStatus status;
    private long unreadCount;
}
