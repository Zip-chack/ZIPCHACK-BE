package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.ListingDto;
import com.Minyou.MINYOU.service.FavoriteService;
import com.Minyou.MINYOU.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {
    private final ListingService listingService;
    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<ListingDto>> getListings(
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = null;
        try {
            userId = extractUserIdFromToken(token);
            System.out.println("매물 목록 조회 - 사용자 ID: " + userId);
        } catch (Exception e) {
            // 토큰이 없거나 유효하지 않은 경우 userId는 null
            System.out.println("매물 목록 조회 - 토큰 없음 또는 유효하지 않음: " + e.getMessage());
        }
        
        List<ListingDto> listings = listingService.getAllListings(roomType, minPrice, maxPrice, search, sort, userId);
        
        // 찜한 매물이 있는지 확인
        long favoriteCount = listings.stream()
                .filter(ListingDto::getIsFavorite)
                .count();
        System.out.println("매물 목록 조회 - 총 " + listings.size() + "개, 찜한 매물 " + favoriteCount + "개");
        
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getListingById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        ListingDto listing = listingService.getListingById(id, null);
        
        // 찜 여부 설정
        try {
            Long userId = extractUserIdFromToken(token);
            if (userId != null) {
                listing.setIsFavorite(favoriteService.isFavorite(id, userId));
            }
        } catch (Exception e) {
            // 토큰이 없거나 유효하지 않은 경우 찜 여부는 false
        }
        
        return ResponseEntity.ok(listing);
    }

    @PostMapping
    public ResponseEntity<?> createListing(
            @RequestBody ListingDto listingDto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            System.out.println("매물 등록 요청 받음 - Building ID: " + 
                (listingDto.getBuilding() != null ? listingDto.getBuilding().getId() : "null"));
            System.out.println("매물 등록 요청 받음 - Title: " + listingDto.getTitle());
            
            Long userId = extractUserIdFromToken(token);
            System.out.println("매물 등록 - 사용자 ID: " + userId);
            
            ListingDto listing = listingService.createListing(listingDto, userId);
            return ResponseEntity.ok(listing);
        } catch (RuntimeException e) {
            System.err.println("매물 등록 실패 (RuntimeException): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("매물 등록 실패 (Exception): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "매물 등록 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingDto> updateListing(
            @PathVariable Long id,
            @RequestBody ListingDto listingDto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            ListingDto listing = listingService.updateListing(id, listingDto, userId);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ListingDto> updateListingStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            String statusStr = statusUpdate.get("status");
            com.Minyou.MINYOU.entity.ListingStatus status = com.Minyou.MINYOU.entity.ListingStatus.valueOf(statusStr);
            
            ListingDto listing = listingService.updateListingStatus(id, status, userId);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            System.err.println("상태 업데이트 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            listingService.deleteListing(id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            boolean isFavorite = favoriteService.toggleFavorite(id, userId);
            return ResponseEntity.ok(Map.of("is_favorite", isFavorite));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<ListingDto>> getFavorites(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            List<ListingDto> favorites = favoriteService.getFavorites(userId);
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Long extractUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token");
        }
        String tokenValue = token.substring(7);
        String[] parts = tokenValue.split("_");
        if (parts.length < 2) {
            throw new RuntimeException("Invalid token format");
        }
        return Long.parseLong(parts[parts.length - 1]);
    }
}