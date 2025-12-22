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
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity { // Extended BaseTimeEntity
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String name; // 실제 이름 (비밀번호 찾기 시 사용)

    @Column(unique = true, nullable = false)
    private String username; // 아이디

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verification_code")
    private String emailVerificationCode;

    @Column(name = "email_verification_code_expiry")
    private LocalDateTime emailVerificationCodeExpiry;

    // createdAt and updatedAt are now inherited from BaseTimeEntity

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private List<Listing> listings = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "favorites",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "listing_id")
    )
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private List<Listing> favoriteListings = new ArrayList<>();
}