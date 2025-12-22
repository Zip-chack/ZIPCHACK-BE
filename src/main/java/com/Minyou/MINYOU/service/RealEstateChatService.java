package com.Minyou.MINYOU.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RealEstateChatService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String AI_SERVICE_URL = System.getenv("AI_SERVICE_URL") != null 
            ? System.getenv("AI_SERVICE_URL") 
            : "http://localhost:8001";

    public RealEstateChatService() {
        log.info("=".repeat(80));
        log.info("RealEstateChatService 초기화");
        log.info("AI_SERVICE_URL: {}", AI_SERVICE_URL);
        log.info("=".repeat(80));
        
        this.webClient = WebClient.builder()
                .baseUrl(AI_SERVICE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 부동산 챗봇 질문에 대한 응답 생성
     * Python FastAPI 서버를 호출
     */
    public String chat(String message, List<Map<String, String>> conversationHistory) {
        try {
            log.info("\n" + "=".repeat(80));
            log.info("부동산 챗봇 요청 전송 시작");
            log.info("URL: {}/api/real-estate-chat", AI_SERVICE_URL);
            log.info("메시지: {}", message);
            log.info("대화 히스토리 길이: {}", conversationHistory != null ? conversationHistory.size() : 0);
            log.info("=".repeat(80));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message);
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                requestBody.put("conversation_history", conversationHistory);
            }

            String response = webClient.post()
                    .uri("/api/real-estate-chat")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60)) // 1분 타임아웃
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                            .filter(throwable -> throwable instanceof java.util.concurrent.TimeoutException))
                    .block();
            
            log.info("Python AI 서버 응답 수신 완료");
            log.info("응답 길이: {}자", response != null ? response.length() : 0);

            JsonNode root = objectMapper.readTree(response);
            if (root.has("response")) {
                String chatResponse = root.get("response").asText();
                log.info("\n" + "=".repeat(80));
                log.info("챗봇 응답:");
                log.info("=".repeat(80));
                log.info(chatResponse);
                log.info("=".repeat(80) + "\n");
                return chatResponse;
            }
            
            log.warn("AI 서버 응답 형식 오류: {}", response);
            return "죄송합니다. 응답을 처리하는 중 오류가 발생했습니다.";
        } catch (Exception e) {
            log.error("\n" + "=".repeat(80));
            log.error("AI 서버 호출 실패!");
            log.error("에러 메시지: {}", e.getMessage());
            log.error("에러 타입: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("원인: {}", e.getCause().getMessage());
            }
            log.error("요청 URL: {}/api/real-estate-chat", AI_SERVICE_URL);
            log.error("=".repeat(80) + "\n");
            e.printStackTrace();
            return "죄송합니다. 챗봇 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.";
        }
    }
}

