package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.ListingDto;
import com.Minyou.MINYOU.entity.Listing;
import com.Minyou.MINYOU.entity.User;
import com.Minyou.MINYOU.mapper.ListingDtoMapper;
import com.Minyou.MINYOU.mapper.ListingMapper;
import com.Minyou.MINYOU.mapper.UserMapper;
import com.Minyou.MINYOU.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {
    private final UserMapper userMapper;
    private final ListingMapper listingMapper;
    private final FavoriteMapper favoriteMapper;
    private final ListingDtoMapper listingDtoMapper;

    /**
     * 찜하기/찜하기 취소 토글
     */
    @Transactional
    public boolean toggleFavorite(Long listingId, Long userId) {
        Listing listing = listingMapper.findById(listingId);
        if (listing == null) {
            throw new RuntimeException("매물을 찾을 수 없습니다.");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 찜 여부 확인
        boolean isFavorite = favoriteMapper.exists(userId, listingId);
        
        if (isFavorite) {
            favoriteMapper.delete(userId, listingId);
        } else {
            favoriteMapper.insert(userId, listingId);
        }

        return !isFavorite; // 변경된 상태 반환
    }

    /**
     * 찜 목록 조회
     */
    public List<ListingDto> getFavorites(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        // 찜한 매물 ID 목록 조회
        List<Long> favoriteIds = listingMapper.findFavoriteListingIdsByUserId(userId);
        
        // 매물 조회
        return favoriteIds.stream()
                .map(listingMapper::findById)
                .filter(java.util.Objects::nonNull)
                .map(listing -> listingDtoMapper.toDto(listing, true))  // 찜 목록이므로 항상 true
                .collect(Collectors.toList());
    }

    /**
     * 특정 매물의 찜 여부 확인
     */
    public boolean isFavorite(Long listingId, Long userId) {
        if (userId == null) {
            return false;
        }

        return favoriteMapper.exists(userId, listingId);
    }
}

