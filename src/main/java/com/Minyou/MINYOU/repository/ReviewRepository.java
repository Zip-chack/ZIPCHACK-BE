package com.Minyou.MINYOU.repository;

import com.Minyou.MINYOU.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByListingId(Long listingId);
    List<Review> findByBuildingId(Long buildingId);
    List<Review> findByUserId(Long userId);
    
    @Query("SELECT DISTINCT r FROM Review r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.listing l " +
           "LEFT JOIN FETCH l.building " +
           "LEFT JOIN FETCH r.building " +
           "WHERE r.user.id = :userId " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByUserIdWithDetails(@Param("userId") Long userId);
    
    /**
     * 특정 위치 반경 내의 건물 리뷰 조회
     * 대략적인 위도/경도 범위로 필터링 (정확한 거리는 서비스 레이어에서 계산)
     */
    @Query("SELECT r FROM Review r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.building b " +
           "WHERE b.lat IS NOT NULL AND b.lng IS NOT NULL " +
           "AND b.lat BETWEEN :minLat AND :maxLat " +
           "AND b.lng BETWEEN :minLng AND :maxLng " +
           "ORDER BY r.createdAt DESC")
    List<Review> findNearbyReviews(
            @Param("minLat") Double minLat, 
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng, 
            @Param("maxLng") Double maxLng);
}

