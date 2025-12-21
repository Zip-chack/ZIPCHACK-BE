package com.Minyou.MINYOU.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {
    private Long id;
    private String title;
    private String content;
    private Double ratingOverall;
    private Double ratingNoise;
    private Double ratingLandlord;
    private Double ratingFacility;
    private LocalDateTime createdAt;
    private UserDto user;
    private ListingDto listing;
    private BuildingDto building;
}

