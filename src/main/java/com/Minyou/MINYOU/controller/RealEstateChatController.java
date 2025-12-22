package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.service.RealEstateChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/real-estate-chat")
@RequiredArgsConstructor
public class RealEstateChatController {
    private final RealEstateChatService realEstateChatService;

    /**
     * 부동산 챗봇 질문에 대한 응답 생성
     */
    @PostMapping
    public ResponseEntity<?> chat(
            @RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> conversationHistory = 
                (List<Map<String, String>>) request.get("conversation_history");
            
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "메시지가 필요합니다."));
            }
            
            String response = realEstateChatService.chat(message, conversationHistory);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "챗봇 요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

