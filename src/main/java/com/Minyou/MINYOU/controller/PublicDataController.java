package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.service.PublicDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public-data")
@RequiredArgsConstructor
@Slf4j
public class PublicDataController {
    private final PublicDataService publicDataService;

    /**
     * 아파트 전월세 실거래가 조회
     * @param lawdCd 법정동코드 (예: 11680 = 서울시 강남구)
     * @param dealYmd 거래년월 (예: 202401, 선택사항 - 없으면 최근 3개월)
     */
    @GetMapping("/apartment-rent")
    public ResponseEntity<?> getApartmentRentData(
            @RequestParam String lawdCd,
            @RequestParam(required = false) String dealYmd) {
        try {
            List<Map<String, Object>> results;
            if (dealYmd != null && !dealYmd.isEmpty()) {
                results = publicDataService.getApartmentRentData(lawdCd, dealYmd);
            } else {
                results = publicDataService.getRecentApartmentRentData(lawdCd);
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("실거래가 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

