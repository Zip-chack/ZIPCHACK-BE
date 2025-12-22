package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.BuildingDto;
import com.Minyou.MINYOU.dto.ListingDto;
import com.Minyou.MINYOU.service.BuildingService;
import com.Minyou.MINYOU.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/buildings", produces = "application/json; charset=UTF-8")
@RequiredArgsConstructor
public class BuildingController {
    private final BuildingService buildingService;
    private final ListingService listingService;

    @GetMapping
    public ResponseEntity<List<BuildingDto>> getBuildings() {
        List<BuildingDto> buildings = buildingService.getAllBuildings();
        return ResponseEntity.ok().header("Content-Type", "application/json; charset=UTF-8").body(buildings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildingDto> getBuildingById(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String roadAddress,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer builtYear) {
        try {
            BuildingDto building = buildingService.getBuildingById(id);
            return ResponseEntity.ok(building);
        } catch (RuntimeException e) {
            // 빌딩이 없고 건물 정보가 제공된 경우 새로 생성
            if (name != null && roadAddress != null && lat != null && lng != null) {
                try {
                    BuildingDto buildingDto = BuildingDto.builder()
                            .name(name)
                            .roadAddress(roadAddress)
                            .lat(lat)
                            .lng(lng)
                            .builtYear(builtYear)
                            .build();
                    BuildingDto createdBuilding = buildingService.getOrCreateBuilding(id, buildingDto);
                    return ResponseEntity.ok(createdBuilding);
                } catch (Exception createException) {
                    return ResponseEntity.badRequest().build();
                }
            }
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<BuildingDto>> searchBuildings(@RequestParam String q) {
        List<BuildingDto> buildings = buildingService.searchBuildings(q);
        return ResponseEntity.ok(buildings);
    }

    @PostMapping
    public ResponseEntity<BuildingDto> createBuilding(@RequestBody BuildingDto buildingDto) {
        try {
            BuildingDto building = buildingService.createBuilding(buildingDto);
            return ResponseEntity.ok(building);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/listings")
    public ResponseEntity<List<ListingDto>> getBuildingListings(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = null;
            try {
                userId = extractUserIdFromToken(token);
            } catch (Exception e) {
                // 토큰이 없거나 유효하지 않은 경우 userId는 null
            }
            List<ListingDto> listings = listingService.getListingsByBuildingId(id, userId);
            return ResponseEntity.ok(listings);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
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
