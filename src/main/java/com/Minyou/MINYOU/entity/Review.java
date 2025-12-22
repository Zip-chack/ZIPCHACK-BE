package com.Minyou.MINYOU.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "rating_overall", nullable = false)
    private Double ratingOverall;

    @Column(name = "rating_noise")
    private Double ratingNoise;

    @Column(name = "rating_landlord")
    private Double ratingLandlord;

    @Column(name = "rating_facility")
    private Double ratingFacility;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    @ToString.Exclude
    @JsonIgnore
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    @ToString.Exclude
    @JsonIgnore
    private Building building;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}