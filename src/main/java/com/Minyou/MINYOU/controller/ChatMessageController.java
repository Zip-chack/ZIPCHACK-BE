package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.ChatMessageDto;
import com.Minyou.MINYOU.dto.ChatMessageResponse;
import com.Minyou.MINYOU.entity.ChatMessage;
import com.Minyou.MINYOU.interceptor.StompJwtAuthInterceptor; // Import the interceptor
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
    private final StompJwtAuthInterceptor stompJwtAuthInterceptor; // Inject the interceptor

    /**
     * WebSocket을 통해 채팅 메시지를 수신하고 온라인 상태인 상대방에게만 실시간으로 전달한다.
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
            // 메시지를 저장하고 저장된 엔티티를 받음
            ChatMessage savedMessage = chatService.saveMessage(roomId, chatMessageDto);

            // 클라이언트에 보낼 응답 DTO 생성
            ChatMessageResponse responseDto = new ChatMessageResponse(savedMessage);
            responseDto.setClientMessageId(chatMessageDto.getClientMessageId()); // 클라이언트 임시 ID를 다시 실어 보냄

            // 상대방 ID 조회
            Long opponentId = savedMessage.getChatRoom().getOwnerId().equals(senderId) 
                            ? savedMessage.getChatRoom().getBuyerId() 
                            : savedMessage.getChatRoom().getOwnerId();

            // 보내는 사람에게 메시지 전송 (항상 온라인 상태)
            messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + senderId, responseDto);

            // 받는 사람이 온라인 상태일 경우에만 메시지 전송
            if (stompJwtAuthInterceptor.getActiveSessions().containsKey(opponentId)) {
                messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + opponentId, responseDto);
            }
            
        } catch (IllegalStateException e) {
            // 거래 완료된 채팅방에 메시지를 보낸 경우 에러 메시지 전송
            ChatMessageDto errorDto = new ChatMessageDto(0L, e.getMessage(), chatMessageDto.getClientMessageId());
            messagingTemplate.convertAndSend("/queue/chat/" + roomId + "/user/" + senderId, errorDto);
        }
    }
}