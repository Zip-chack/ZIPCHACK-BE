package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.*;
import com.Minyou.MINYOU.security.TokenUtil;
import com.Minyou.MINYOU.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final TokenUtil tokenUtil; // JWT 토큰에서 사용자 정보를 추출하는 유틸

    /**
     * 매물 ID를 기준으로 채팅방을 생성하거나 기존 채팅방을 반환한다.
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
            @RequestBody CreateChatRoomRequest request,
            @RequestHeader("Authorization") String token
    ) {
        Long buyerId = tokenUtil.extractUserIdFromToken(token);
        ChatRoomResponse response = chatService.createOrGetRoom(request.getListingId(), buyerId);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그인한 사용자가 참여 중인 모든 채팅방 목록을 조회한다.
     */
    @GetMapping("/my-rooms")
    public ResponseEntity<List<MyChatRoomResponse>> getMyChatRooms(
            @RequestHeader("Authorization") String token
    ) {
        Long userId = tokenUtil.extractUserIdFromToken(token);
        List<MyChatRoomResponse> rooms = chatService.getMyChatRooms(userId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomResponse> getChatRoomDetails(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = tokenUtil.extractUserIdFromToken(token);
        ChatRoomResponse response = chatService.getRoomById(roomId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 채팅방의 전체 메시지 목록을 조회한다.
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getChatMessages(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = tokenUtil.extractUserIdFromToken(token);
        List<ChatMessageResponse> messages = chatService.getChatMessages(roomId, userId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 채팅방을 거래 완료 상태로 변경한다.
     */
    @PatchMapping("/rooms/{roomId}/complete")
    public ResponseEntity<Void> completeChatRoom(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = tokenUtil.extractUserIdFromToken(token);
        chatService.completeChat(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 채팅방을 삭제한다.
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String token
    ) {
        Long userId = tokenUtil.extractUserIdFromToken(token);
        chatService.deleteChatRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }
}
