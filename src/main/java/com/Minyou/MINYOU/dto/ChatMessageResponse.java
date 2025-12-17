package com.Minyou.MINYOU.dto;

import com.Minyou.MINYOU.entity.ChatMessage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {
    private Long messageId;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;

    public ChatMessageResponse(ChatMessage message) {
        this.messageId = message.getId();
        this.senderId = message.getSenderId();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }
}