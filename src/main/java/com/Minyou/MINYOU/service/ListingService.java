package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.*;
import com.Minyou.MINYOU.entity.Building;
import com.Minyou.MINYOU.entity.Listing;
import com.Minyou.MINYOU.entity.User;
import com.Minyou.MINYOU.mapper.ListingDtoMapper;
import com.Minyou.MINYOU.repository.BuildingRepository;
import com.Minyou.MINYOU.repository.ListingRepository;
import com.Minyou.MINYOU.repository.ReviewRepository;
import com.Minyou.MINYOU.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingService {
    private final ListingRepository listingRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ListingDtoMapper listingDtoMapper;
    private final EntityManager entityManager;

    public List<ListingDto> getAllListings(String roomType, Integer minPrice, Integer maxPrice, String search, String sort, Long userId) {
        // 조인 쿼리를 사용하여 한 번의 쿼리로 매물과 찜 여부를 함께 가져옴
        if (userId != null) {
            return getAllListingsWithFavorite(roomType, minPrice, maxPrice, search, sort, userId);
        }

        // userId가 없는 경우 기존 방식 사용
        List<Listing> listings;
        if (search != null && !search.isEmpty()) {
            listings = listingRepository.searchByQuery(search);
        } else if (roomType != null && !roomType.isEmpty()) {
            listings = listingRepository.findByRoomType(roomType);
        } else if (minPrice != null && maxPrice != null) {
            listings = listingRepository.findByMonthlyRentBetween(minPrice, maxPrice);
        } else if (minPrice != null) {
            // minPrice만 있는 경우 (100+ 케이스)
            listings = listingRepository.findAll().stream()
                    .filter(l -> l.getMonthlyRent() != null && l.getMonthlyRent() >= minPrice)
                    .collect(Collectors.toList());
        } else {
            listings = listingRepository.findAll();
        }

        // 정렬 적용
        List<ListingDto> dtos = listings.stream()
                .map(listingDtoMapper::toDto)
                .collect(Collectors.toList());
        
        return applySorting(dtos, sort);
    }

    /**
     * 조인 쿼리를 사용하여 매물과 찜 여부를 한 번에 조회
     */
    private List<ListingDto> getAllListingsWithFavorite(String roomType, Integer minPrice, Integer maxPrice, String search, String sort, Long userId) {
        String sql;
        Query query;
        
        // 정렬 조건 결정
        String orderBy = getOrderByClause(sort);

        if (search != null && !search.isEmpty()) {
            sql = 
                "SELECT l.*, " +
                "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
                "FROM listings l " +
                "LEFT JOIN buildings b ON l.building_id = b.id " +
                "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
                "WHERE b.road_address LIKE CONCAT('%', :query, '%') OR l.title LIKE CONCAT('%', :query, '%') " +
                orderBy;
            query = entityManager.createNativeQuery(sql);
            query.setParameter("query", search);
            query.setParameter("userId", userId);
        } else if (roomType != null && !roomType.isEmpty()) {
            sql = 
                "SELECT l.*, " +
                "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
                "FROM listings l " +
                "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
                "WHERE l.room_type = :roomType " +
                orderBy;
            query = entityManager.createNativeQuery(sql);
            query.setParameter("roomType", roomType);
            query.setParameter("userId", userId);
        } else if (minPrice != null && maxPrice != null) {
            sql = 
                "SELECT l.*, " +
                "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
                "FROM listings l " +
                "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
                "WHERE l.monthly_rent BETWEEN :min AND :max " +
                orderBy;
            query = entityManager.createNativeQuery(sql);
            query.setParameter("min", minPrice);
            query.setParameter("max", maxPrice);
            query.setParameter("userId", userId);
        } else if (minPrice != null) {
            // minPrice만 있는 경우 (100+ 케이스)
            sql = 
                "SELECT l.*, " +
                "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
                "FROM listings l " +
                "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
                "WHERE l.monthly_rent >= :min " +
                orderBy;
            query = entityManager.createNativeQuery(sql);
            query.setParameter("min", minPrice);
            query.setParameter("userId", userId);
        } else {
            sql = 
                "SELECT l.*, " +
                "CASE WHEN f.user_id IS NOT NULL THEN true ELSE false END as is_favorite " +
                "FROM listings l " +
                "LEFT JOIN favorites f ON l.id = f.listing_id AND f.user_id = :userId " +
                orderBy;
            query = entityManager.createNativeQuery(sql);
            query.setParameter("userId", userId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return new ArrayList<>();
        }

        // 결과에서 ID와 isFavorite를 추출
        List<Long> listingIds = new ArrayList<>();
        java.util.Map<Long, Boolean> favoriteMap = new java.util.HashMap<>();
        
        for (Object[] row : results) {
            // 첫 번째 컬럼이 id (Number 타입으로 반환됨 - Long 또는 BigInteger)
            Long listingId = ((Number) row[0]).longValue();
            listingIds.add(listingId);
            
            // 마지막 컬럼이 is_favorite
            Object isFavoriteObj = row[row.length - 1];
            Boolean isFavorite = false;
            if (isFavoriteObj instanceof Boolean) {
                isFavorite = (Boolean) isFavoriteObj;
            } else if (isFavoriteObj instanceof Number) {
                isFavorite = ((Number) isFavoriteObj).intValue() == 1;
            }
            favoriteMap.put(listingId, isFavorite);
        }

        // 한 번의 쿼리로 모든 Listing 조회
        List<Listing> listings = listingRepository.findAllById(listingIds);
        
        // 순서 유지를 위해 Map으로 변환
        java.util.Map<Long, Listing> listingMap = listings.stream()
                .collect(Collectors.toMap(Listing::getId, listing -> listing));

        // 원래 순서대로 DTO 변환
        List<ListingDto> listingDtos = listingIds.stream()
                .map(id -> {
                    Listing listing = listingMap.get(id);
                    if (listing == null) {
                        log.warn("매물 ID {}를 찾을 수 없습니다.", id);
                        return null;
                    }
                    Boolean isFavorite = favoriteMap.getOrDefault(id, false);
                    return listingDtoMapper.toDto(listing, isFavorite);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("사용자 {}의 매물 조회 결과: {}개, 찜한 매물: {}개", 
                userId, listingDtos.size(), 
                listingDtos.stream().filter(ListingDto::getIsFavorite).count());

        // 모든 정렬을 Java에서 재적용하여 순서 보장
        return applySorting(listingDtos, sort);
    }

    /**
     * 정렬 조건에 따른 ORDER BY 절 생성
     */
    private String getOrderByClause(String sort) {
        if (sort == null || sort.isEmpty() || "latest".equals(sort)) {
            return "ORDER BY l.created_at DESC";
        } else if ("price_low".equals(sort)) {
            return "ORDER BY l.monthly_rent ASC";
        } else if ("price_high".equals(sort)) {
            return "ORDER BY l.monthly_rent DESC";
        } else if ("rating".equals(sort)) {
            // 평점 정렬은 Java에서 처리 (리뷰 조인 필요)
            return "ORDER BY l.created_at DESC";
        } else {
            return "ORDER BY l.created_at DESC";
        }
    }

    /**
     * 정렬 적용 (평점 정렬 등 Java에서 처리해야 하는 경우)
     */
    private List<ListingDto> applySorting(List<ListingDto> dtos, String sort) {
        if (sort == null || sort.isEmpty() || "latest".equals(sort)) {
            // 최신순: created_at 기준 내림차순
            return dtos.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());
        } else if ("price_low".equals(sort)) {
            return dtos.stream()
                    .sorted((a, b) -> {
                        Integer priceA = a.getMonthlyRent() != null ? a.getMonthlyRent() : 0;
                        Integer priceB = b.getMonthlyRent() != null ? b.getMonthlyRent() : 0;
                        return priceA.compareTo(priceB);
                    })
                    .collect(Collectors.toList());
        } else if ("price_high".equals(sort)) {
            return dtos.stream()
                    .sorted((a, b) -> {
                        Integer priceA = a.getMonthlyRent() != null ? a.getMonthlyRent() : 0;
                        Integer priceB = b.getMonthlyRent() != null ? b.getMonthlyRent() : 0;
                        return priceB.compareTo(priceA);
                    })
                    .collect(Collectors.toList());
        } else if ("rating".equals(sort)) {
            // 평점 정렬 - 리뷰 평균 계산 필요
            return dtos.stream()
                    .sorted((a, b) -> {
                        Double ratingA = a.getRating() != null ? a.getRating() : 0.0;
                        Double ratingB = b.getRating() != null ? b.getRating() : 0.0;
                        return ratingB.compareTo(ratingA);
                    })
                    .collect(Collectors.toList());
        } else {
            return dtos;
        }
    }

    public ListingDto getListingById(Long id, Long userId) {
        Listing listing = listingRepository.findByIdWithDetails(id);
        if (listing == null) {
            throw new RuntimeException("매물을 찾을 수 없습니다.");
        }

        // 찜 여부는 Controller에서 설정하도록 변경
        return listingDtoMapper.toDto(listing, false);
    }

    @Transactional
    public ListingDto createListing(ListingDto listingDto, Long userId) {
        if (listingDto == null) {
            throw new RuntimeException("매물 정보가 없습니다.");
        }
        
        if (listingDto.getBuilding() == null || listingDto.getBuilding().getId() == null) {
            throw new RuntimeException("건물 정보가 없습니다.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. 로그인이 필요합니다."));

        Building building = buildingRepository.findById(listingDto.getBuilding().getId())
                .orElseThrow(() -> new RuntimeException("건물을 찾을 수 없습니다. 건물 ID: " + listingDto.getBuilding().getId()));

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
        return listingDtoMapper.toDto(listing);
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
        return listingDtoMapper.toDto(listing);
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
    public ListingDto updateListingStatus(Long id, com.Minyou.MINYOU.entity.ListingStatus status, Long userId) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다."));

        if (!listing.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        listing.setStatus(status);
        listing = listingRepository.save(listing);
        return listingDtoMapper.toDto(listing);
    }

    /**
     * Building ID로 매물 목록 조회
     */
    public List<ListingDto> getListingsByBuildingId(Long buildingId, Long userId) {
        List<Listing> listings = listingRepository.findByBuildingId(buildingId);
        
        if (userId != null) {
            // 찜 여부 포함하여 조회
            return listings.stream()
                    .map(listing -> {
                        ListingDto dto = listingDtoMapper.toDto(listing);
                        // 찜 여부는 FavoriteService를 통해 확인해야 하지만, 
                        // 일단 기본값으로 false 설정 (나중에 개선 가능)
                        dto.setIsFavorite(false);
                        return dto;
                    })
                    .collect(Collectors.toList());
        }
        
        return listings.stream()
                .map(listingDtoMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 ID로 매물 목록 조회
     */
    public List<ListingDto> getUserListings(Long userId) {
        List<Listing> listings = listingRepository.findByUserIdWithDetails(userId);
        return listings.stream()
                .map(listingDtoMapper::toDto)
                .collect(Collectors.toList());
    }
}