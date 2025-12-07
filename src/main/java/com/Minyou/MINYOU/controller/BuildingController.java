package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.dto.BuildingDto;
import com.Minyou.MINYOU.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingController {
    private final BuildingService buildingService;

    @GetMapping
    public ResponseEntity<List<BuildingDto>> getBuildings() {
        List<BuildingDto> buildings = buildingService.getAllBuildings();
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildingDto> getBuildingById(@PathVariable Long id) {
        try {
            BuildingDto building = buildingService.getBuildingById(id);
            return ResponseEntity.ok(building);
        } catch (RuntimeException e) {
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
}
