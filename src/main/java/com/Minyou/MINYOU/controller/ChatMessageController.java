package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.ChatMessageDto;
import com.Minyou.MINYOU.service.ChatService;
import com.Minyou.MINYOU.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/chat/send/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            ChatMessageDto chatMessageDto,
            Authentication authentication
    ) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long senderId = userPrincipal.getId();
        chatMessageDto.setSenderId(senderId);

        try {
            Long opponentId = chatService.saveMessageAndGetOpponentId(roomId, chatMessageDto);

            // 보내는 사람
            messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + senderId, chatMessageDto);
            // 받는 사람
            messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + opponentId, chatMessageDto);
        } catch (IllegalStateException e) {
            // 채팅 완료 후
            ChatMessageDto errorDto = new ChatMessageDto(0L, e.getMessage());
            messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + senderId, errorDto);
        }
    }
}