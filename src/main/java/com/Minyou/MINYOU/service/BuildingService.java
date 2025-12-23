package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.BuildingDto;
import com.Minyou.MINYOU.entity.Building;
import com.Minyou.MINYOU.mapper.BuildingMapper;
import com.Minyou.MINYOU.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingService {
    private final BuildingMapper buildingMapper;
    private final ReviewMapper reviewMapper;

    public List<BuildingDto> getAllBuildings() {
        return buildingMapper.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public BuildingDto getBuildingById(Long id) {
        Building building = buildingMapper.findById(id);
        if (building == null) {
            throw new RuntimeException("건물을 찾을 수 없습니다.");
        }
        return convertToDto(building);
    }

    @Transactional
    public BuildingDto getOrCreateBuilding(Long id, BuildingDto buildingDto) {
        Building building = buildingMapper.findById(id);
        if (building != null) {
            return convertToDto(building);
        }
        
        // 빌딩이 없으면 새로 생성 (지정된 ID 사용)
        buildingMapper.insertWithId(
                id,
                buildingDto.getName(),
                buildingDto.getRoadAddress(),
                buildingDto.getLat(),
                buildingDto.getLng(),
                buildingDto.getBuiltYear()
        );
        // 저장 후 다시 조회
        building = buildingMapper.findById(id);
        if (building == null) {
            throw new RuntimeException("건물 생성 후 조회 실패");
        }
        return convertToDto(building);
    }

    public List<BuildingDto> searchBuildings(String query) {
        return buildingMapper.searchByQuery(query).stream()
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

        buildingMapper.insert(building);
        return convertToDto(building);
    }

    private BuildingDto convertToDto(Building building) {
        // 리뷰 조회
        List<com.Minyou.MINYOU.entity.Review> reviews = reviewMapper.findByBuildingId(building.getId());
        double rating = reviews.stream()
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
                .reviewCount(reviews.size())
                .build();
    }
}