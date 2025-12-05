package com.Minyou.MINYOU.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDto {
    private Long id;
    private String title;
    private String roomType;
    private Integer deposit;
    private Integer monthlyRent;
    private Integer maintenanceFee;
    private Double areaM2;
    private Integer floor;
    private String image;
    private Double rating;
    private Integer reviewCount;
    private Boolean isFavorite;
    private BuildingDto building;
}