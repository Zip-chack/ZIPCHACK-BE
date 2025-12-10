package com.Minyou.MINYOU.repository;

import com.Minyou.MINYOU.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    @Query("SELECT l FROM Listing l WHERE l.building.roadAddress LIKE %:query% OR l.title LIKE %:query%")
    List<Listing> searchByQuery(@Param("query") String query);

    List<Listing> findByRoomType(String roomType);

    @Query("SELECT l FROM Listing l WHERE l.monthlyRent BETWEEN :min AND :max")
    List<Listing> findByMonthlyRentBetween(@Param("min") Integer min, @Param("max") Integer max);

    /**
     * 사용자가 찜한 매물 ID 목록 조회
     */
    @Query("SELECT f.id FROM User u JOIN u.favoriteListings f WHERE u.id = :userId")
    Set<Long> findFavoriteListingIdsByUserId(@Param("userId") Long userId);

    /**
     * 전체 매물 조회 (찜 여부 포함)
     */
    @Query(value = 
        "SELECT l.*, " +
        "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
        "FROM listings l " +
        "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
        "ORDER BY l.created_at DESC", 
        nativeQuery = true)
    List<Object[]> findAllWithFavoriteStatus(@Param("userId") Long userId);

    /**
     * 검색 쿼리 (찜 여부 포함)
     */
    @Query(value = 
        "SELECT l.*, " +
        "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
        "FROM listings l " +
        "LEFT JOIN buildings b ON l.building_id = b.id " +
        "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
        "WHERE b.road_address LIKE CONCAT('%', :query, '%') OR l.title LIKE CONCAT('%', :query, '%') " +
        "ORDER BY l.created_at DESC", 
        nativeQuery = true)
    List<Object[]> searchByQueryWithFavoriteStatus(@Param("query") String query, @Param("userId") Long userId);

    /**
     * 방 타입별 조회 (찜 여부 포함)
     */
    @Query(value = 
        "SELECT l.*, " +
        "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
        "FROM listings l " +
        "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
        "WHERE l.room_type = :roomType " +
        "ORDER BY l.created_at DESC", 
        nativeQuery = true)
    List<Object[]> findByRoomTypeWithFavoriteStatus(@Param("roomType") String roomType, @Param("userId") Long userId);

    /**
     * 가격 범위별 조회 (찜 여부 포함)
     */
    @Query(value = 
        "SELECT l.*, " +
        "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
        "FROM listings l " +
        "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
        "WHERE l.monthly_rent BETWEEN :min AND :max " +
        "ORDER BY l.created_at DESC", 
        nativeQuery = true)
    List<Object[]> findByMonthlyRentBetweenWithFavoriteStatus(@Param("min") Integer min, @Param("max") Integer max, @Param("userId") Long userId);

    /**
     * Building ID로 매물 조회
     */
    List<Listing> findByBuildingId(Long buildingId);
}