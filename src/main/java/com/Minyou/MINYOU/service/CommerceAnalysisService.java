package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.ReviewDto;
import com.Minyou.MINYOU.mapper.ReviewMapper;
import com.Minyou.MINYOU.entity.Review;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommerceAnalysisService {
    private final KakaoMapService kakaoMapService;
    private final ReviewMapper reviewMapper;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String AI_SERVICE_URL = System.getenv("AI_SERVICE_URL") != null 
            ? System.getenv("AI_SERVICE_URL") 
            : "http://localhost:8001";

    public CommerceAnalysisService(KakaoMapService kakaoMapService, ReviewMapper reviewMapper) {
        this.kakaoMapService = kakaoMapService;
        this.reviewMapper = reviewMapper;
        
        log.info("=".repeat(80));
        log.info("CommerceAnalysisService 초기화");
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

        // 주변 리뷰 데이터 조회
        List<Map<String, Object>> reviewsData = null;
        try {
            // 위도/경도 범위 계산 (대략적인 계산: 1도 ≈ 111km)
            double latRange = radius / 111000.0; // 미터를 도로 변환
            double lngRange = radius / (111000.0 * Math.cos(Math.toRadians(lat))); // 위도에 따른 경도 보정
            
            double minLat = lat - latRange;
            double maxLat = lat + latRange;
            double minLng = lng - lngRange;
            double maxLng = lng + lngRange;
            
            List<Review> reviews = reviewMapper.findNearbyReviews(minLat, maxLat, minLng, maxLng);
            
            // 정확한 거리 계산 및 필터링 (하버사인 공식)
            reviews = reviews.stream()
                    .filter(review -> {
                        if (review.getBuilding() == null) return false;
                        double reviewLat = review.getBuilding().getLat();
                        double reviewLng = review.getBuilding().getLng();
                        
                        // 하버사인 공식으로 거리 계산
                        double earthRadius = 6371000; // 미터
                        double dLat = Math.toRadians(reviewLat - lat);
                        double dLng = Math.toRadians(reviewLng - lng);
                        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                                Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(reviewLat)) *
                                Math.sin(dLng / 2) * Math.sin(dLng / 2);
                        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                        double distance = earthRadius * c;
                        
                        return distance <= radius;
                    })
                    .collect(Collectors.toList());
            
            reviewsData = reviews.stream()
                    .map(review -> {
                        Map<String, Object> reviewMap = new HashMap<>();
                        reviewMap.put("title", review.getTitle());
                        reviewMap.put("content", review.getContent());
                        reviewMap.put("ratingOverall", review.getRatingOverall());
                        reviewMap.put("ratingNoise", review.getRatingNoise());
                        reviewMap.put("ratingLandlord", review.getRatingLandlord());
                        reviewMap.put("ratingFacility", review.getRatingFacility());
                        if (review.getBuilding() != null) {
                            reviewMap.put("buildingName", review.getBuilding().getName());
                        }
                        return reviewMap;
                    })
                    .collect(Collectors.toList());
            log.info("주변 리뷰 {}개 조회 완료", reviews.size());
        } catch (Exception e) {
            log.warn("리뷰 데이터 조회 실패 (계속 진행): {}", e.getMessage());
            e.printStackTrace();
            reviewsData = List.of();
        }

        try {
            // AI 서버에 요청
            log.info("\n" + "=".repeat(80));
            log.info("Python AI 서버로 요청 전송 시작");
            log.info("URL: {}/api/commerce-analysis", AI_SERVICE_URL);
            log.info("위도: {}, 경도: {}, 반경: {}m", lat, lng, radius);
            log.info("리뷰 개수: {}", reviewsData != null ? reviewsData.size() : 0);
            log.info("=".repeat(80));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("lat", lat);
            requestBody.put("lng", lng);
            requestBody.put("radius", radius);
            requestBody.put("commerce_info", commerceInfo);
            requestBody.put("reviews", reviewsData != null ? reviewsData : List.of());

            String response = webClient.post()
                    .uri("/api/commerce-analysis")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(180)) // 3분 타임아웃
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                            .filter(throwable -> throwable instanceof java.util.concurrent.TimeoutException))
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
