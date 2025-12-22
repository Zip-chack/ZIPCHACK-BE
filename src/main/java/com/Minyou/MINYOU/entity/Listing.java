package com.Minyou.MINYOU.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "room_type", nullable = false)
    private String roomType;

    @Column(nullable = false)
    private Integer deposit;

    @Column(name = "monthly_rent", nullable = false)
    private Integer monthlyRent;

    @Column(name = "maintenance_fee")
    private Integer maintenanceFee;

    @Column(name = "area_m2")
    private Double areaM2;

    @Column(nullable = false)
    private Integer floor;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    @ToString.Exclude
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    @ManyToMany(mappedBy = "favoriteListings")
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private List<User> favoritedBy = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingStatus status = ListingStatus.AVAILABLE;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
