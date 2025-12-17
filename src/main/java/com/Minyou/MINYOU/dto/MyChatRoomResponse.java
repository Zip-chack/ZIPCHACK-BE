package com.Minyou.MINYOU.dto;

import com.Minyou.MINYOU.entity.ChatRoomStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MyChatRoomResponse {
    private Long roomId;
    private Long opponentId;
    private String lastMessage;
    private ChatRoomStatus status;

    @Builder
    public MyChatRoomResponse(Long roomId, Long opponentId, String lastMessage, ChatRoomStatus status) {
        this.roomId = roomId;
        this.opponentId = opponentId;
        this.lastMessage = lastMessage;
        this.status = status;
    }
}
