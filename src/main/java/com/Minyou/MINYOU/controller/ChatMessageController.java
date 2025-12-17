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

    /**
     * WebSocket을 통해 채팅 메시지를 수신하고 상대방에게 실시간으로 전달한다.
     */
    @MessageMapping("/chat/send/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            ChatMessageDto chatMessageDto,
            Authentication authentication
    ) {
        // 인증 정보에서 로그인한 사용자 ID 추출
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long senderId = userPrincipal.getId();
        chatMessageDto.setSenderId(senderId);

        try {
            // 메시지를 저장하고 상대방 사용자 ID 조회
            Long opponentId = chatService.saveMessageAndGetOpponentId(roomId, chatMessageDto);

            // 보내는 사람에게 메시지 전송
            messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + senderId, chatMessageDto);
            // 받는 사람에게 메시지 전송
            messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + opponentId, chatMessageDto);
        } catch (IllegalStateException e) {
            // 거래 완료된 채팅방에 메시지를 보낸 경우 에러 메시지 전송
            ChatMessageDto errorDto = new ChatMessageDto(0L, e.getMessage());
            messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + senderId, errorDto);
        }
    }
}