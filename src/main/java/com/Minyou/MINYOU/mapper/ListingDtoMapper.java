package com.Minyou.MINYOU.mapper;

import com.Minyou.MINYOU.dto.BuildingDto;
import com.Minyou.MINYOU.dto.ListingDto;
import com.Minyou.MINYOU.entity.Listing;
import org.springframework.stereotype.Component;

/**
 * Listing 엔티티와 ListingDto 간의 변환을 담당하는 Mapper
 * 순환 참조를 방지하기 위해 별도 컴포넌트로 분리
 */
@Component
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

        return ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .roomType(listing.getRoomType())
                .deposit(listing.getDeposit())
                .monthlyRent(listing.getMonthlyRent())
                .maintenanceFee(listing.getMaintenanceFee())
                .areaM2(listing.getAreaM2())
                .floor(listing.getFloor())
                .image(listing.getImageUrl())
                .rating(rating)
                .reviewCount(listing.getReviews().size())
                .isFavorite(isFavorite)
                .building(BuildingDto.builder()
                        .id(listing.getBuilding().getId())
                        .name(listing.getBuilding().getName())
                        .roadAddress(listing.getBuilding().getRoadAddress())
                        .lat(listing.getBuilding().getLat())
                        .lng(listing.getBuilding().getLng())
                        .builtYear(listing.getBuilding().getBuiltYear())
                        .build())
                .build();
    }
}

