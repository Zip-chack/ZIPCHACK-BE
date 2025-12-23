package com.Minyou.MINYOU.mapper;

import com.Minyou.MINYOU.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
    List<Review> findAll();
    Review findById(Long id);
    List<Review> findByListingId(@Param("listingId") Long listingId);
    List<Review> findByBuildingId(@Param("buildingId") Long buildingId);
    List<Review> findByUserId(@Param("userId") Long userId);
    List<Review> findByUserIdWithDetails(@Param("userId") Long userId);
    List<Review> findNearbyReviews(@Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
                                   @Param("minLng") Double minLng, @Param("maxLng") Double maxLng);
    void insert(Review review);
    void update(Review review);
    void delete(Long id);
}

