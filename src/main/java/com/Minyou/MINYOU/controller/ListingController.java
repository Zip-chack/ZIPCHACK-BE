package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.ListingDto;
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

    @GetMapping
    public ResponseEntity<List<ListingDto>> getListings(
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String search,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            List<ListingDto> listings = listingService.getAllListings(roomType, minPrice, maxPrice, search);
            return ResponseEntity.ok(listings);
        } catch (Exception e) {
            Long userId = null;
            List<ListingDto> listings = listingService.getAllListings(roomType, minPrice, maxPrice, search);
            return ResponseEntity.ok(listings);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDto> getListingById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            ListingDto listing = listingService.getListingById(id, userId);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            ListingDto listing = listingService.getListingById(id, null);
            return ResponseEntity.ok(listing);
        }
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
            ListingDto listing = listingService.toggleFavorite(id, userId);
            return ResponseEntity.ok(Map.of("is_favorite", listing.getIsFavorite()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<ListingDto>> getFavorites(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            List<ListingDto> favorites = listingService.getFavorites(userId);
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