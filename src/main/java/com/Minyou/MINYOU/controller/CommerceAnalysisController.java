package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.service.CommerceAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/commerce-analysis")
@RequiredArgsConstructor
public class CommerceAnalysisController {
    private final CommerceAnalysisService commerceAnalysisService;

    /**
     * 주변 상권 분석 레포트 생성 (Python AI 서버 호출)
     */
    @GetMapping("/report")
    public ResponseEntity<?> generateCommerceReport(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "500") Integer radius) {
        try {
            String report = commerceAnalysisService.generateCommerceReport(lat, lng, radius);
            return ResponseEntity.ok(Map.of("report", report));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
