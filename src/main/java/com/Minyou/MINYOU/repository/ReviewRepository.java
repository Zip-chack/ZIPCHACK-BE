package com.Minyou.MINYOU.repository;

import com.Minyou.MINYOU.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByListingId(Long listingId);
    List<Review> findByBuildingId(Long buildingId);
}

