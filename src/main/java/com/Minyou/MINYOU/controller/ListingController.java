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
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = null;
        try {
            userId = extractUserIdFromToken(token);
            System.out.println("매물 목록 조회 - 사용자 ID: " + userId);
        } catch (Exception e) {
            // 토큰이 없거나 유효하지 않은 경우 userId는 null
            System.out.println("매물 목록 조회 - 토큰 없음 또는 유효하지 않음: " + e.getMessage());
        }
        
        List<ListingDto> listings = listingService.getAllListings(roomType, minPrice, maxPrice, search, userId);
        
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
    public ResponseEntity<ListingDto> createListing(
            @RequestBody ListingDto listingDto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            ListingDto listing = listingService.createListing(listingDto, userId);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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