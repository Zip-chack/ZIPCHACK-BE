package com.Minyou.MINYOU.repository;

import com.Minyou.MINYOU.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    @Query("SELECT l FROM Listing l WHERE l.building.roadAddress LIKE %:query% OR l.title LIKE %:query%")
    List<Listing> searchByQuery(@Param("query") String query);

    List<Listing> findByRoomType(String roomType);

    @Query("SELECT l FROM Listing l WHERE l.monthlyRent BETWEEN :min AND :max")
    List<Listing> findByMonthlyRentBetween(@Param("min") Integer min, @Param("max") Integer max);
}