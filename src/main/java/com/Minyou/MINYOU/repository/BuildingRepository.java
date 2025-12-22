package com.Minyou.MINYOU.repository;

import com.Minyou.MINYOU.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {
    @Query("SELECT b FROM Building b WHERE b.name LIKE %:query% OR b.roadAddress LIKE %:query%")
    List<Building> searchByQuery(@Param("query") String query);
    
    @Modifying
    @Query(value = "INSERT INTO buildings (id, name, road_address, lat, lng, built_year) VALUES (:id, :name, :roadAddress, :lat, :lng, :builtYear)", nativeQuery = true)
    void insertBuildingWithId(@Param("id") Long id, @Param("name") String name, @Param("roadAddress") String roadAddress, @Param("lat") Double lat, @Param("lng") Double lng, @Param("builtYear") Integer builtYear);
}