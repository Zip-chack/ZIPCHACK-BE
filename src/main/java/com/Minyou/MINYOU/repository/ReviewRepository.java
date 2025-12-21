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
}

