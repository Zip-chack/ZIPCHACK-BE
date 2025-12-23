package com.Minyou.MINYOU.mapper;

import com.Minyou.MINYOU.entity.Building;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BuildingMapper {
    List<Building> findAll();
    Building findById(Long id);
    List<Building> searchByQuery(@Param("query") String query);
    void insert(Building building);
    void update(Building building);
    void delete(Long id);
    void insertWithId(@Param("id") Long id, @Param("name") String name, 
                      @Param("roadAddress") String roadAddress, 
                      @Param("lat") Double lat, @Param("lng") Double lng, 
                      @Param("builtYear") Integer builtYear);
}

