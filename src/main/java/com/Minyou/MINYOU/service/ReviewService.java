package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.BuildingDto;
import com.Minyou.MINYOU.dto.ListingDto;
import com.Minyou.MINYOU.dto.ReviewDto;
import com.Minyou.MINYOU.dto.UserDto;
import com.Minyou.MINYOU.entity.Building;
import com.Minyou.MINYOU.entity.Listing;
import com.Minyou.MINYOU.entity.Review;
import com.Minyou.MINYOU.entity.User;
import com.Minyou.MINYOU.repository.BuildingRepository;
import com.Minyou.MINYOU.repository.ListingRepository;
import com.Minyou.MINYOU.repository.ReviewRepository;
import com.Minyou.MINYOU.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ListingRepository listingRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;

    public List<ReviewDto> getListingReviews(Long listingId) {
        return reviewRepository.findByListingId(listingId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ReviewDto> getBuildingReviews(Long buildingId) {
        return reviewRepository.findByBuildingId(buildingId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDto createListingReview(Long listingId, ReviewDto reviewDto, Long userId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Review review = Review.builder()
                .title(reviewDto.getTitle())
                .content(reviewDto.getContent())
                .ratingOverall(reviewDto.getRatingOverall())
                .ratingNoise(reviewDto.getRatingNoise())
                .ratingLandlord(reviewDto.getRatingLandlord())
                .ratingFacility(reviewDto.getRatingFacility())
                .listing(listing)
                .user(user)
                .build();

        review = reviewRepository.save(review);
        return convertToDto(review);
    }

    @Transactional
    public ReviewDto createBuildingReview(Long buildingId, ReviewDto reviewDto, Long userId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("건물을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Review review = Review.builder()
                .title(reviewDto.getTitle())
                .content(reviewDto.getContent())
                .ratingOverall(reviewDto.getRatingOverall())
                .ratingNoise(reviewDto.getRatingNoise())
                .ratingLandlord(reviewDto.getRatingLandlord())
                .ratingFacility(reviewDto.getRatingFacility())
                .building(building)
                .user(user)
                .build();

        review = reviewRepository.save(review);
        return convertToDto(review);
    }

    @Transactional
    public ReviewDto updateReview(Long reviewId, ReviewDto reviewDto, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        review.setTitle(reviewDto.getTitle());
        review.setContent(reviewDto.getContent());
        review.setRatingOverall(reviewDto.getRatingOverall());
        review.setRatingNoise(reviewDto.getRatingNoise());
        review.setRatingLandlord(reviewDto.getRatingLandlord());
        review.setRatingFacility(reviewDto.getRatingFacility());

        review = reviewRepository.save(review);
        return convertToDto(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        reviewRepository.delete(review);
    }

    private ReviewDto convertToDto(Review review) {
        ReviewDto.ReviewDtoBuilder builder = ReviewDto.builder()
                .id(review.getId())
                .title(review.getTitle())
                .content(review.getContent())
                .ratingOverall(review.getRatingOverall())
                .ratingNoise(review.getRatingNoise())
                .ratingLandlord(review.getRatingLandlord())
                .ratingFacility(review.getRatingFacility())
                .createdAt(review.getCreatedAt())
                .user(UserDto.builder()
                        .id(review.getUser().getId())
                        .email(review.getUser().getEmail())
                        .nickname(review.getUser().getNickname())
                        .build());

        // Listing 정보 추가 (Building 정보 포함)
        if (review.getListing() != null) {
            Listing listing = review.getListing();
            ListingDto.ListingDtoBuilder listingBuilder = ListingDto.builder()
                    .id(listing.getId())
                    .title(listing.getTitle());
            
            // Listing의 Building 정보 추가
            Building listingBuilding = listing.getBuilding();
            if (listingBuilding != null) {
                BuildingDto buildingDto = BuildingDto.builder()
                        .id(listingBuilding.getId())
                        .name(listingBuilding.getName())
                        .roadAddress(listingBuilding.getRoadAddress())
                        .build();
                listingBuilder.building(buildingDto);
                System.out.println("  [DTO 변환] Listing의 Building 매핑 완료: " + buildingDto.getId() + " - " + buildingDto.getName());
            } else {
                System.out.println("  [DTO 변환] Listing의 Building이 null입니다.");
            }
            
            ListingDto listingDto = listingBuilder.build();
            builder.listing(listingDto);
            System.out.println("  [DTO 변환] Listing 매핑 완료: " + listingDto.getId() + " - " + listingDto.getTitle());
        } else {
            System.out.println("  [DTO 변환] Review의 Listing이 null입니다.");
        }

        // Building 정보 추가
        if (review.getBuilding() != null) {
            Building building = review.getBuilding();
            BuildingDto buildingDto = BuildingDto.builder()
                    .id(building.getId())
                    .name(building.getName())
                    .roadAddress(building.getRoadAddress())
                    .build();
            builder.building(buildingDto);
            System.out.println("  [DTO 변환] Building 매핑 완료: " + buildingDto.getId() + " - " + buildingDto.getName());
        } else {
            System.out.println("  [DTO 변환] Review의 Building이 null입니다.");
        }

        return builder.build();
    }

    /**
     * 사용자 ID로 리뷰 목록 조회
     */
    public List<ReviewDto> getUserReviews(Long userId) {
        List<Review> reviews = reviewRepository.findByUserIdWithDetails(userId);
        System.out.println("=== 사용자 " + userId + "의 리뷰 조회 ===");
        System.out.println("리뷰 개수: " + reviews.size());
        
        for (Review review : reviews) {
            System.out.println("리뷰 ID: " + review.getId());
            System.out.println("  - Listing: " + (review.getListing() != null ? review.getListing().getId() + " (" + review.getListing().getTitle() + ")" : "null"));
            System.out.println("  - Building: " + (review.getBuilding() != null ? review.getBuilding().getId() + " (" + review.getBuilding().getName() + ")" : "null"));
            if (review.getListing() != null && review.getListing().getBuilding() != null) {
                System.out.println("  - Listing의 Building: " + review.getListing().getBuilding().getId() + " (" + review.getListing().getBuilding().getName() + ")");
            }
        }
        
        List<ReviewDto> dtos = reviews.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        for (ReviewDto dto : dtos) {
            System.out.println("DTO 리뷰 ID: " + dto.getId());
            System.out.println("  - Listing DTO: " + (dto.getListing() != null ? dto.getListing().getId() + " (" + dto.getListing().getTitle() + ")" : "null"));
            System.out.println("  - Building DTO: " + (dto.getBuilding() != null ? dto.getBuilding().getId() + " (" + dto.getBuilding().getName() + ")" : "null"));
        }
        
        return dtos;
    }
}

