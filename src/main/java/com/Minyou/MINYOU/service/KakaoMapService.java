package com.Minyou.MINYOU.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KakaoMapService {
    private static final String KAKAO_API_BASE_URL = "https://dapi.kakao.com";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private String apiKey;

    public KakaoMapService() {
        Dotenv dotenv = Dotenv.load();
        this.apiKey = dotenv.get("KAKAO_MAP_REST_API_KEY");
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            log.error("KAKAO_MAP_REST_API_KEY가 설정되지 않았습니다. 카카오맵 API 기능을 사용할 수 없습니다.");
        } else {
            log.info("카카오맵 REST API 키가 설정되었습니다.");
        }
        
        this.webClient = WebClient.builder()
                .baseUrl(KAKAO_API_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + (this.apiKey != null ? this.apiKey : ""))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 주소로 좌표 검색
     */
    public Map<String, Object> searchAddress(String address) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("KAKAO_MAP_REST_API_KEY가 설정되지 않아 주소 검색을 수행할 수 없습니다.");
            throw new RuntimeException("카카오맵 API 키가 설정되지 않았습니다. KAKAO_MAP_REST_API_KEY 환경변수를 설정해주세요.");
        }
        
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            
            // 에러 체크
            if (root.has("error")) {
                String errorMsg = root.get("error").get("message").asText();
                log.error("카카오맵 API 에러: {}", errorMsg);
                throw new RuntimeException("카카오맵 API 오류: " + errorMsg);
            }
            
            JsonNode documents = root.get("documents");

            if (documents != null && documents.isArray() && documents.size() > 0) {
                JsonNode firstDoc = documents.get(0);
                Map<String, Object> result = new HashMap<>();
                result.put("address", firstDoc.get("address_name").asText());
                result.put("roadAddress", firstDoc.get("road_address") != null 
                    ? firstDoc.get("road_address").get("address_name").asText() 
                    : firstDoc.get("address_name").asText());
                result.put("lat", Double.parseDouble(firstDoc.get("y").asText()));
                result.put("lng", Double.parseDouble(firstDoc.get("x").asText()));
                return result;
            }
            return null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("주소 검색 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주소 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 키워드로 장소 검색
     */
    public List<Map<String, Object>> searchKeyword(String keyword, Double lat, Double lng, Integer radius) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("KAKAO_MAP_REST_API_KEY가 설정되지 않아 키워드 검색을 수행할 수 없습니다.");
            throw new RuntimeException("카카오맵 API 키가 설정되지 않았습니다. KAKAO_MAP_REST_API_KEY 환경변수를 설정해주세요.");
        }
        
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/v2/local/search/keyword.json");
                        uriBuilder.queryParam("query", keyword);
                        if (lat != null && lng != null) {
                            uriBuilder.queryParam("x", lng);
                            uriBuilder.queryParam("y", lat);
                        }
                        if (radius != null) {
                            uriBuilder.queryParam("radius", radius);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            
            // 에러 체크
            if (root.has("error")) {
                String errorMsg = root.get("error").get("message").asText();
                log.error("카카오맵 API 에러: {}", errorMsg);
                throw new RuntimeException("카카오맵 API 오류: " + errorMsg);
            }
            
            JsonNode documents = root.get("documents");

            List<Map<String, Object>> results = new ArrayList<>();
            if (documents != null && documents.isArray()) {
                for (JsonNode doc : documents) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", doc.get("id").asText());
                    result.put("placeName", doc.get("place_name").asText());
                    result.put("address", doc.get("address_name").asText());
                    result.put("roadAddress", doc.get("road_address_name").asText());
                    result.put("lat", Double.parseDouble(doc.get("y").asText()));
                    result.put("lng", Double.parseDouble(doc.get("x").asText()));
                    result.put("phone", doc.has("phone") ? doc.get("phone").asText() : "");
                    result.put("category", doc.has("category_name") ? doc.get("category_name").asText() : "");
                    results.add(result);
                }
            }
            return results;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("키워드 검색 실패: {}", e.getMessage(), e);
            throw new RuntimeException("키워드 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 좌표를 주소로 변환
     */
    public Map<String, Object> coordToAddress(Double lat, Double lng) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("KAKAO_MAP_REST_API_KEY가 설정되지 않아 좌표 변환을 수행할 수 없습니다.");
            throw new RuntimeException("카카오맵 API 키가 설정되지 않았습니다. KAKAO_MAP_REST_API_KEY 환경변수를 설정해주세요.");
        }
        
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/geo/coord2address.json")
                            .queryParam("x", lng)
                            .queryParam("y", lat)
                            .queryParam("input_coord", "WGS84")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            
            // 에러 체크
            if (root.has("error")) {
                String errorMsg = root.get("error").get("message").asText();
                log.error("카카오맵 API 에러: {}", errorMsg);
                throw new RuntimeException("카카오맵 API 오류: " + errorMsg);
            }
            
            JsonNode documents = root.get("documents");

            if (documents != null && documents.isArray() && documents.size() > 0) {
                JsonNode firstDoc = documents.get(0);
                Map<String, Object> result = new HashMap<>();
                result.put("address", firstDoc.get("address") != null 
                    ? firstDoc.get("address").get("address_name").asText() 
                    : "");
                result.put("roadAddress", firstDoc.get("road_address") != null 
                    ? firstDoc.get("road_address").get("address_name").asText() 
                    : "");
                return result;
            }
            return null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("좌표 변환 실패: {}", e.getMessage(), e);
            throw new RuntimeException("좌표 변환 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 지도 화면 영역(bounds) 내의 건물 검색
     * 영역을 격자로 나누어 여러 카테고리로 검색
     */
    public List<Map<String, Object>> searchBuildingsInBounds(
            Double swLat, Double swLng, Double neLat, Double neLng) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("KAKAO_MAP_REST_API_KEY가 설정되지 않아 건물 검색을 수행할 수 없습니다.");
            throw new RuntimeException("카카오맵 API 키가 설정되지 않았습니다. KAKAO_MAP_REST_API_KEY 환경변수를 설정해주세요.");
        }

        List<Map<String, Object>> allResults = new ArrayList<>();
        Map<String, Map<String, Object>> uniqueResults = new HashMap<>(); // 중복 제거용

        // 영역의 중심점과 반경 계산
        double centerLat = (swLat + neLat) / 2.0;
        double centerLng = (swLng + neLng) / 2.0;
        
        // 대략적인 반경 계산 (미터 단위)
        double latDiff = neLat - swLat;
        double lngDiff = neLng - swLng;
        double radius = Math.max(latDiff * 111000, lngDiff * 111000 * Math.cos(Math.toRadians(centerLat))); // 대략적인 미터 변환
        int radiusMeters = (int) Math.min(radius, 20000); // 최대 20km

        // 검색할 카테고리 키워드들
        String[] keywords = {"원룸", "오피스텔", "아파트", "빌라", "주택", "부동산"};

        // 각 키워드로 검색
        for (String keyword : keywords) {
            try {
                List<Map<String, Object>> results = searchKeyword(keyword, centerLat, centerLng, radiusMeters);
                
                // 경계 내에 있는 것만 필터링하고 중복 제거
                for (Map<String, Object> result : results) {
                    Double lat = (Double) result.get("lat");
                    Double lng = (Double) result.get("lng");
                    
                    // 경계 내에 있는지 확인
                    if (lat != null && lng != null && 
                        lat >= swLat && lat <= neLat && 
                        lng >= swLng && lng <= neLng) {
                        
                        String placeId = (String) result.get("id");
                        if (placeId != null && !uniqueResults.containsKey(placeId)) {
                            uniqueResults.put(placeId, result);
                        }
                    }
                }
                
                // API 호출 제한을 고려하여 약간의 지연
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("키워드 '{}' 검색 중 오류: {}", keyword, e.getMessage());
            }
        }

        allResults.addAll(uniqueResults.values());
        log.info("경계 내 건물 검색 완료: {}개 건물 발견", allResults.size());
        return allResults;
    }

    /**
     * 주변 상권 정보 조회 (편의점, 카페, 마트, 음식점, 약국 등)
     * @param lat 위도
     * @param lng 경도
     * @param radius 반경 (미터, 기본값 500m)
     * @return 상권 정보 맵 (카테고리별 개수)
     */
    public Map<String, Object> getNearbyCommerceInfo(Double lat, Double lng, Integer radius) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("KAKAO_MAP_REST_API_KEY가 설정되지 않아 상권 정보 조회를 수행할 수 없습니다.");
            throw new RuntimeException("카카오맵 API 키가 설정되지 않았습니다. KAKAO_MAP_REST_API_KEY 환경변수를 설정해주세요.");
        }

        if (radius == null) {
            radius = 500; // 기본 반경 500m
        }

        Map<String, Object> commerceInfo = new HashMap<>();
        
        // 검색할 카테고리 키워드들
        Map<String, String> categories = new HashMap<>();
        categories.put("convenienceStore", "편의점");
        categories.put("cafe", "카페");
        categories.put("mart", "마트");
        categories.put("restaurant", "음식점");
        categories.put("pharmacy", "약국");
        categories.put("bank", "은행");
        categories.put("hospital", "병원");
        categories.put("subway", "지하철역");

        // 각 카테고리별로 검색하여 개수 집계
        for (Map.Entry<String, String> entry : categories.entrySet()) {
            String categoryKey = entry.getKey();
            String keyword = entry.getValue();
            
            try {
                List<Map<String, Object>> results = searchKeyword(keyword, lat, lng, radius);
                commerceInfo.put(categoryKey, results.size());
                
                // API 호출 제한을 고려하여 약간의 지연
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("카테고리 '{}' 검색 중 오류: {}", keyword, e.getMessage());
                commerceInfo.put(categoryKey, 0);
            }
        }

        log.debug("주변 상권 정보 조회 완료: 위도 {}, 경도 {}, 반경 {}m", lat, lng, radius);
        return commerceInfo;
    }
}

