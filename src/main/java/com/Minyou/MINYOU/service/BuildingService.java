package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.BuildingDto;
import com.Minyou.MINYOU.entity.Building;
import com.Minyou.MINYOU.repository.BuildingRepository;
import com.Minyou.MINYOU.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingService {
    private final BuildingRepository buildingRepository;
    private final ReviewRepository reviewRepository;

    public List<BuildingDto> getAllBuildings() {
        return buildingRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public BuildingDto getBuildingById(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("건물을 찾을 수 없습니다."));
        return convertToDto(building);
    }

    public List<BuildingDto> searchBuildings(String query) {
        return buildingRepository.searchByQuery(query).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BuildingDto createBuilding(BuildingDto buildingDto) {
        Building building = Building.builder()
                .name(buildingDto.getName())
                .roadAddress(buildingDto.getRoadAddress())
                .lat(buildingDto.getLat())
                .lng(buildingDto.getLng())
                .builtYear(buildingDto.getBuiltYear())
                .build();

        building = buildingRepository.save(building);
        return convertToDto(building);
    }

    private BuildingDto convertToDto(Building building) {
        double rating = building.getReviews().stream()
                .mapToDouble(review -> review.getRatingOverall())
                .average()
                .orElse(0.0);

        return BuildingDto.builder()
                .id(building.getId())
                .name(building.getName())
                .roadAddress(building.getRoadAddress())
                .lat(building.getLat())
                .lng(building.getLng())
                .builtYear(building.getBuiltYear())
                .rating(rating)
                .reviewCount(building.getReviews().size())
                .build();
    }
}