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
public class ListingDto {
    private Long id;
    private String title;
    private String description;
    private String roomType;
    private Integer deposit;
    private Integer monthlyRent;
    private Integer maintenanceFee;
    private Double areaM2;
    private Integer floor;
    private String image;
    private Double rating;
    private Integer reviewCount;
    private String status;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    private BuildingDto building;
    private UserDto owner;
}