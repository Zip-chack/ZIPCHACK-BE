package com.Minyou.MINYOU.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CommerceAnalysisService {
    private final KakaoMapService kakaoMapService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String AI_SERVICE_URL = System.getenv("AI_SERVICE_URL") != null 
            ? System.getenv("AI_SERVICE_URL") 
            : "http://localhost:8001";

    public CommerceAnalysisService(KakaoMapService kakaoMapService) {
        this.kakaoMapService = kakaoMapService;
        
        log.info("=".repeat(80));
        log.info("CommerceAnalysisService 초기화");
        log.info("AI_SERVICE_URL: {}", AI_SERVICE_URL);
        log.info("=".repeat(80));
        
        this.webClient = WebClient.builder()
                .baseUrl(AI_SERVICE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 주변 상권 정보를 기반으로 AI 분석 레포트 생성
     * Python FastAPI 서버를 호출
     */
    public String generateCommerceReport(Double lat, Double lng, Integer radius) {
        // 주변 상권 정보 조회
        Map<String, Object> commerceInfo;
        try {
            commerceInfo = kakaoMapService.getNearbyCommerceInfo(lat, lng, radius);
        } catch (Exception e) {
            log.error("상권 정보 조회 실패: {}", e.getMessage());
            return generateDefaultReport(null, lat, lng, radius);
        }

        try {
            // AI 서버에 요청
            log.info("\n" + "=".repeat(80));
            log.info("Python AI 서버로 요청 전송 시작");
            log.info("URL: {}/api/commerce-analysis", AI_SERVICE_URL);
            log.info("위도: {}, 경도: {}, 반경: {}m", lat, lng, radius);
            log.info("=".repeat(80));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("lat", lat);
            requestBody.put("lng", lng);
            requestBody.put("radius", radius);
            requestBody.put("commerce_info", commerceInfo);

            String response = webClient.post()
                    .uri("/api/commerce-analysis")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Python AI 서버 응답 수신 완료");
            log.info("응답 길이: {}자", response != null ? response.length() : 0);

            JsonNode root = objectMapper.readTree(response);
            if (root.has("report")) {
                String report = root.get("report").asText();
                // LLM 생성 내용을 콘솔에 출력
                log.info("\n" + "=".repeat(80));
                log.info("LLM 생성 레포트:");
                log.info("=".repeat(80));
                log.info(report);
                log.info("=".repeat(80) + "\n");
                return report;
            }
            
            log.warn("AI 서버 응답 형식 오류: {}", response);
            return generateDefaultReport(commerceInfo, lat, lng, radius);
        } catch (Exception e) {
            log.error("\n" + "=".repeat(80));
            log.error("AI 서버 호출 실패!");
            log.error("에러 메시지: {}", e.getMessage());
            log.error("에러 타입: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("원인: {}", e.getCause().getMessage());
            }
            log.error("요청 URL: {}/api/commerce-analysis", AI_SERVICE_URL);
            log.error("기본 레포트로 대체합니다.");
            log.error("=".repeat(80) + "\n");
            e.printStackTrace();
            return generateDefaultReport(commerceInfo, lat, lng, radius);
        }
    }

    private String generateDefaultReport(Map<String, Object> commerceInfo, Double lat, Double lng, Integer radius) {
        if (commerceInfo == null) {
            return String.format(
                "반경 %dm 내의 주변 상권 정보를 조회할 수 없습니다.",
                radius
            );
        }
        
        StringBuilder report = new StringBuilder();
        report.append(String.format("반경 %dm 내의 주변 상권 분석 결과입니다:\n\n", radius));
        
        int totalCount = 0;
        totalCount += (Integer) commerceInfo.getOrDefault("convenienceStore", 0);
        totalCount += (Integer) commerceInfo.getOrDefault("cafe", 0);
        totalCount += (Integer) commerceInfo.getOrDefault("mart", 0);
        totalCount += (Integer) commerceInfo.getOrDefault("restaurant", 0);
        totalCount += (Integer) commerceInfo.getOrDefault("pharmacy", 0);
        totalCount += (Integer) commerceInfo.getOrDefault("bank", 0);
        totalCount += (Integer) commerceInfo.getOrDefault("hospital", 0);
        totalCount += (Integer) commerceInfo.getOrDefault("subway", 0);
        
        report.append(String.format("총 %d개의 시설이 확인되었습니다. ", totalCount));
        
        // 주요 시설 강조
        int convenienceStore = (Integer) commerceInfo.getOrDefault("convenienceStore", 0);
        int cafe = (Integer) commerceInfo.getOrDefault("cafe", 0);
        int restaurant = (Integer) commerceInfo.getOrDefault("restaurant", 0);
        int subway = (Integer) commerceInfo.getOrDefault("subway", 0);
        
        if (subway > 0) {
            report.append("지하철역이 접근 가능한 위치에 있어 교통이 편리합니다. ");
        }
        if (restaurant > 5) {
            report.append("다양한 음식점이 있어 식사 옵션이 풍부합니다. ");
        } else if (restaurant > 0) {
            report.append("음식점이 있어 기본적인 식사는 가능합니다. ");
        }
        if (cafe > 3) {
            report.append("카페가 많아 생활이 편리합니다. ");
        }
        if (convenienceStore > 2) {
            report.append("편의점이 여러 곳 있어 일상생활이 편리합니다.");
        }
        
        report.append("\n\n※ 더 상세한 AI 분석 레포트를 보시려면 AI 서버가 실행 중인지 확인해주세요.");
        
        return report.toString();
    }
}
