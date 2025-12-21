package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.ListingDto;
import com.Minyou.MINYOU.dto.ReviewDto;
import com.Minyou.MINYOU.entity.User;
import com.Minyou.MINYOU.repository.UserRepository;
import com.Minyou.MINYOU.service.ChatService;
import com.Minyou.MINYOU.service.ListingService;
import com.Minyou.MINYOU.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ListingService listingService;
    private final ReviewService reviewService;
    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody User user) {
        if (!userRepository.findById(userId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        user.setId(userId);
        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userRepository.deleteById(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 로그인한 사용자의 정보 요약 조회 (마이페이지용)
     */
    @GetMapping("/me/summary")
    public ResponseEntity<Map<String, Object>> getMySummary(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            log.info("마이페이지 요약 조회 시작 - token: {}", token != null ? "존재" : "없음");
            Long userId = extractUserIdFromToken(token);
            log.info("추출된 userId: {}", userId);
            
            // 사용자 정보
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 통계 정보
            long listingCount = listingService.getUserListings(userId).size();
            long reviewCount = reviewService.getUserReviews(userId).size();
            
            // 읽지 않은 메시지 개수 조회
            long unreadMessageCount = chatService.getUnreadMessageCount(userId);
            
            log.info("마이페이지 요약 조회 - userId: {}, listingCount: {}, reviewCount: {}, unreadMessageCount: {}", 
                    userId, listingCount, reviewCount, unreadMessageCount);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("userId", user.getId());
            summary.put("email", user.getEmail());
            summary.put("nickname", user.getNickname());
            summary.put("listingCount", listingCount);
            summary.put("reviewCount", reviewCount);
            summary.put("unreadMessageCount", unreadMessageCount);
            
            log.info("반환할 summary: {}", summary);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("마이페이지 요약 조회 실패", e);
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 현재 로그인한 사용자의 매물 목록 조회
     */
    @GetMapping("/me/listings")
    public ResponseEntity<List<ListingDto>> getMyListings(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            List<ListingDto> listings = listingService.getUserListings(userId);
            return ResponseEntity.ok(listings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 현재 로그인한 사용자의 리뷰 목록 조회
     */
    @GetMapping("/me/reviews")
    public ResponseEntity<List<ReviewDto>> getMyReviews(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            List<ReviewDto> reviews = reviewService.getUserReviews(userId);
            log.info("사용자 {}의 리뷰 목록 조회: {}개", userId, reviews.size());
            for (ReviewDto review : reviews) {
                log.info("리뷰 ID: {}, Listing: {}, Building: {}", 
                    review.getId(), 
                    review.getListing() != null ? review.getListing().getId() : "null",
                    review.getBuilding() != null ? review.getBuilding().getId() : "null");
            }
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            log.error("리뷰 목록 조회 실패", e);
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

