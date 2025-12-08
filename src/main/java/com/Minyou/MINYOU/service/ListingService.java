package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.*;
import com.Minyou.MINYOU.entity.Building;
import com.Minyou.MINYOU.entity.Listing;
import com.Minyou.MINYOU.entity.User;
import com.Minyou.MINYOU.repository.BuildingRepository;
import com.Minyou.MINYOU.repository.ListingRepository;
import com.Minyou.MINYOU.repository.ReviewRepository;
import com.Minyou.MINYOU.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingService {
    private final ListingRepository listingRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public List<ListingDto> getAllListings(String roomType, Integer minPrice, Integer maxPrice, String search) {
        List<Listing> listings;

        if (search != null && !search.isEmpty()) {
            listings = listingRepository.searchByQuery(search);
        } else if (roomType != null && !roomType.isEmpty()) {
            listings = listingRepository.findByRoomType(roomType);
        } else if (minPrice != null && maxPrice != null) {
            listings = listingRepository.findByMonthlyRentBetween(minPrice, maxPrice);
        } else {
            listings = listingRepository.findAll();
        }

        return listings.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ListingDto getListingById(Long id, Long userId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다."));

        ListingDto dto = convertToDto(listing);
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                dto.setIsFavorite(user.getFavoriteListings().contains(listing));
            }
        }
        return dto;
    }

    @Transactional
    public ListingDto createListing(ListingDto listingDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Building building = buildingRepository.findById(listingDto.getBuilding().getId())
                .orElseThrow(() -> new RuntimeException("건물을 찾을 수 없습니다."));

        Listing listing = Listing.builder()
                .title(listingDto.getTitle())
                .roomType(listingDto.getRoomType())
                .deposit(listingDto.getDeposit())
                .monthlyRent(listingDto.getMonthlyRent())
                .maintenanceFee(listingDto.getMaintenanceFee())
                .areaM2(listingDto.getAreaM2())
                .floor(listingDto.getFloor())
                .imageUrl(listingDto.getImage())
                .building(building)
                .user(user)
                .build();

        listing = listingRepository.save(listing);
        return convertToDto(listing);
    }

    @Transactional
    public ListingDto updateListing(Long id, ListingDto listingDto, Long userId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다."));

        if (!listing.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        listing.setTitle(listingDto.getTitle());
        listing.setRoomType(listingDto.getRoomType());
        listing.setDeposit(listingDto.getDeposit());
        listing.setMonthlyRent(listingDto.getMonthlyRent());
        listing.setMaintenanceFee(listingDto.getMaintenanceFee());
        listing.setAreaM2(listingDto.getAreaM2());
        listing.setFloor(listingDto.getFloor());
        listing.setImageUrl(listingDto.getImage());

        listing = listingRepository.save(listing);
        return convertToDto(listing);
    }

    @Transactional
    public void deleteListing(Long id, Long userId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다."));

        if (!listing.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        listingRepository.delete(listing);
    }

    @Transactional
    public ListingDto toggleFavorite(Long listingId, Long userId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        boolean isFavorite = user.getFavoriteListings().contains(listing);
        if (isFavorite) {
            user.getFavoriteListings().remove(listing);
        } else {
            user.getFavoriteListings().add(listing);
        }
        userRepository.save(user);

        ListingDto dto = convertToDto(listing);
        dto.setIsFavorite(!isFavorite);
        return dto;
    }

    public List<ListingDto> getFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return user.getFavoriteListings().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ListingDto convertToDto(Listing listing) {
        double rating = listing.getReviews().stream()
                .mapToDouble(review -> review.getRatingOverall())
                .average()
                .orElse(0.0);

        return ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .roomType(listing.getRoomType())
                .deposit(listing.getDeposit())
                .monthlyRent(listing.getMonthlyRent())
                .maintenanceFee(listing.getMaintenanceFee())
                .areaM2(listing.getAreaM2())
                .floor(listing.getFloor())
                .image(listing.getImageUrl())
                .rating(rating)
                .reviewCount(listing.getReviews().size())
                .isFavorite(false)
                .building(BuildingDto.builder()
                        .id(listing.getBuilding().getId())
                        .name(listing.getBuilding().getName())
                        .roadAddress(listing.getBuilding().getRoadAddress())
                        .lat(listing.getBuilding().getLat())
                        .lng(listing.getBuilding().getLng())
                        .builtYear(listing.getBuilding().getBuiltYear())
                        .build())
                .build();
    }
}