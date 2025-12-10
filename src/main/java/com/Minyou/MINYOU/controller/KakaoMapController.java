package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.service.KakaoMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
public class KakaoMapController {
    private final KakaoMapService kakaoMapService;

    /**
     * 주소로 좌표 검색
     */
    @GetMapping("/address")
    public ResponseEntity<?> searchAddress(@RequestParam String query) {
        try {
            Map<String, Object> result = kakaoMapService.searchAddress(query);
            if (result != null) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 키워드로 장소 검색
     */
    @GetMapping("/keyword")
    public ResponseEntity<?> searchKeyword(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "2000") Integer radius) {
        try {
            List<Map<String, Object>> results = kakaoMapService.searchKeyword(query, lat, lng, radius);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 좌표를 주소로 변환
     */
    @GetMapping("/coord2address")
    public ResponseEntity<?> coordToAddress(
            @RequestParam Double lat,
            @RequestParam Double lng) {
        try {
            Map<String, Object> result = kakaoMapService.coordToAddress(lat, lng);
            if (result != null) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 지도 화면 영역 내의 건물 검색
     * swLat, swLng: 남서쪽 경계 (South-West)
     * neLat, neLng: 북동쪽 경계 (North-East)
     */
    @GetMapping("/buildings-in-bounds")
    public ResponseEntity<?> searchBuildingsInBounds(
            @RequestParam Double swLat,
            @RequestParam Double swLng,
            @RequestParam Double neLat,
            @RequestParam Double neLng) {
        try {
            List<Map<String, Object>> results = kakaoMapService.searchBuildingsInBounds(
                    swLat, swLng, neLat, neLng);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 주변 상권 정보 조회 (편의점, 카페, 마트 등)
     */
    @GetMapping("/nearby-commerce")
    public ResponseEntity<?> getNearbyCommerceInfo(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "500") Integer radius) {
        try {
            Map<String, Object> results = kakaoMapService.getNearbyCommerceInfo(lat, lng, radius);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

