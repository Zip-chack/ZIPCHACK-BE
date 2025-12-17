package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.*;
import com.Minyou.MINYOU.service.ChatService;
import com.Minyou.MINYOU.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final TokenUtil tokenUtil; // 토큰 유틸 주입

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
            @RequestBody CreateChatRoomRequest request,
            @RequestHeader("Authorization") String token
    ) {
        Long buyerId = tokenUtil.extractUserIdFromToken(token);
        ChatRoomResponse response = chatService.createOrGetRoom(request.getListingId(), buyerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<List<MyChatRoomResponse>> getMyChatRooms(
            @RequestHeader("Authorization") String token // Changed
    ) {
        Long userId = tokenUtil.extractUserIdFromToken(token); // Changed
        List<MyChatRoomResponse> rooms = chatService.getMyChatRooms(userId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getChatMessages(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String token // Changed
    ) {
        Long userId = tokenUtil.extractUserIdFromToken(token); // Changed
        List<ChatMessageResponse> messages = chatService.getChatMessages(roomId, userId);
        return ResponseEntity.ok(messages);
    }

    @PatchMapping("/rooms/{roomId}/complete")
    public ResponseEntity<Void> completeChatRoom(
            @PathVariable Long roomId,
            @RequestHeader("Authorization") String token // Changed
    ) {
        Long userId = tokenUtil.extractUserIdFromToken(token); // Changed
        chatService.completeChat(roomId, userId);
        return ResponseEntity.ok().build();
    }
}
