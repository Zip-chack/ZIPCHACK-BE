package com.Minyou.MINYOU.mapper;

import com.Minyou.MINYOU.dto.BuildingDto;
import com.Minyou.MINYOU.dto.ListingDto;
import com.Minyou.MINYOU.dto.UserDto;
import com.Minyou.MINYOU.entity.Listing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listing 엔티티와 ListingDto 간의 변환을 담당하는 Mapper
 * 순환 참조를 방지하기 위해 별도 컴포넌트로 분리
 */
@Component
@Slf4j
public class ListingDtoMapper {

    /**
     * Listing 엔티티를 ListingDto로 변환
     * 
     * @param listing 변환할 Listing 엔티티
     * @return 변환된 ListingDto
     */
    public ListingDto toDto(Listing listing) {
        return toDto(listing, false);
    }

    /**
     * Listing 엔티티를 ListingDto로 변환 (찜 여부 포함)
     * 
     * @param listing 변환할 Listing 엔티티
     * @param isFavorite 찜 여부
     * @return 변환된 ListingDto
     */
    public ListingDto toDto(Listing listing, boolean isFavorite) {
        double rating = listing.getReviews().stream()
                .mapToDouble(review -> review.getRatingOverall())
                .average()
                .orElse(0.0);

        String imageUrl = listing.getImageUrl();
        String defaultImageUrl = "https://minyou-images.s3.ap-northeast-2.amazonaws.com/listings/%E1%84%80%E1%85%B5%E1%84%87%E1%85%A9%E1%86%AB%E1%84%8B%E1%85%B5%E1%84%86%E1%85%B5%E1%84%8C%E1%85%B5.png";
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            log.debug("매물 {} 이미지 URL: {}", listing.getId(), imageUrl);
        } else {
            log.debug("매물 {} 이미지 URL이 없습니다. 기본 이미지 사용: {}", listing.getId(), defaultImageUrl);
            imageUrl = defaultImageUrl;
        }
        
        return ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .roomType(listing.getRoomType())
                .deposit(listing.getDeposit())
                .monthlyRent(listing.getMonthlyRent())
                .maintenanceFee(listing.getMaintenanceFee())
                .areaM2(listing.getAreaM2())
                .floor(listing.getFloor())
                .image(imageUrl)
                .status(listing.getStatus().name())
                .rating(rating)
                .reviewCount(listing.getReviews().size())
                .isFavorite(isFavorite)
                .createdAt(listing.getCreatedAt())
                .building(BuildingDto.builder()
                        .id(listing.getBuilding().getId())
                        .name(listing.getBuilding().getName())
                        .roadAddress(listing.getBuilding().getRoadAddress())
                        .lat(listing.getBuilding().getLat())
                        .lng(listing.getBuilding().getLng())
                        .builtYear(listing.getBuilding().getBuiltYear())
                        .build())
                .owner(UserDto.builder()
                        .id(listing.getUser().getId())
                        .email(listing.getUser().getEmail())
                        .nickname(listing.getUser().getNickname())
                        .build())
                .build();
    }
}

