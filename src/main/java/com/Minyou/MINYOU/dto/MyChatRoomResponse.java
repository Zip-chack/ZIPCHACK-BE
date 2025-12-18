package com.Minyou.MINYOU.dto;

import com.Minyou.MINYOU.entity.ChatRoomStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MyChatRoomResponse {
    private Long roomId;
    private Long targetUserId;
    private String targetUserNickname;
    private String lastMessage;
    private ChatRoomStatus status;

    @Builder
    public MyChatRoomResponse(Long roomId, Long targetUserId, String targetUserNickname, String lastMessage, ChatRoomStatus status) {
        this.roomId = roomId;
        this.targetUserId = targetUserId;
        this.targetUserNickname = targetUserNickname;
        this.lastMessage = lastMessage;
        this.status = status;
    }
}
