package com.Minyou.MINYOU.mapper;

import com.Minyou.MINYOU.entity.Listing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface ListingMapper {
    // 기본 CRUD
    List<Listing> findAll();
    Listing findById(Long id);
    Listing findByIdWithDetails(Long id);
    void insert(Listing listing);
    void update(Listing listing);
    void delete(Long id);
    
    // 검색 및 필터링
    List<Listing> searchByQuery(@Param("query") String query);
    List<Listing> findByRoomType(@Param("roomType") String roomType);
    List<Listing> findByMonthlyRentBetween(@Param("min") Integer min, @Param("max") Integer max);
    List<Listing> findByBuildingId(@Param("buildingId") Long buildingId);
    List<Listing> findByUserId(@Param("userId") Long userId);
    List<Listing> findByUserIdWithDetails(@Param("userId") Long userId);
    
    // 찜하기 관련
    List<Long> findFavoriteListingIdsByUserId(@Param("userId") Long userId);
    List<Map<String, Object>> findAllWithFavoriteStatus(@Param("userId") Long userId);
    List<Map<String, Object>> searchByQueryWithFavoriteStatus(@Param("query") String query, @Param("userId") Long userId);
    List<Map<String, Object>> findByRoomTypeWithFavoriteStatus(@Param("roomType") String roomType, @Param("userId") Long userId);
    List<Map<String, Object>> findByMonthlyRentBetweenWithFavoriteStatus(@Param("min") Integer min, @Param("max") Integer max, @Param("userId") Long userId);
}

