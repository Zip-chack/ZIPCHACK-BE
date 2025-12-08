package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.ReviewDto;
import com.Minyou.MINYOU.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/listings/{listingId}/reviews")
    public ResponseEntity<List<ReviewDto>> getListingReviews(@PathVariable Long listingId) {
        List<ReviewDto> reviews = reviewService.getListingReviews(listingId);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/listings/{listingId}/reviews")
    public ResponseEntity<ReviewDto> createListingReview(
            @PathVariable Long listingId,
            @RequestBody ReviewDto reviewDto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            ReviewDto review = reviewService.createListingReview(listingId, reviewDto, userId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/buildings/{buildingId}/reviews")
    public ResponseEntity<List<ReviewDto>> getBuildingReviews(@PathVariable Long buildingId) {
        List<ReviewDto> reviews = reviewService.getBuildingReviews(buildingId);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/buildings/{buildingId}/reviews")
    public ResponseEntity<ReviewDto> createBuildingReview(
            @PathVariable Long buildingId,
            @RequestBody ReviewDto reviewDto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            ReviewDto review = reviewService.createBuildingReview(buildingId, reviewDto, userId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewDto reviewDto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            ReviewDto review = reviewService.updateReview(reviewId, reviewDto, userId);
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            reviewService.deleteReview(reviewId, userId);
            return ResponseEntity.ok().build();
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

