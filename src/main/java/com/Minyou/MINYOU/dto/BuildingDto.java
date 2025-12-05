package com.Minyou.MINYOU.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingDto {
    private Long id;
    private String name;
    private String roadAddress;
    private Double lat;
    private Double lng;
    private Integer builtYear;
    private Double rating;
    private Integer reviewCount;
}