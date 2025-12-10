package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.ListingDto;
import com.Minyou.MINYOU.entity.Listing;
import com.Minyou.MINYOU.entity.User;
import com.Minyou.MINYOU.mapper.ListingDtoMapper;
import com.Minyou.MINYOU.repository.ListingRepository;
import com.Minyou.MINYOU.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ListingDtoMapper listingDtoMapper;

    /**
     * 찜하기/찜하기 취소 토글
     */
    @Transactional
    public boolean toggleFavorite(Long listingId, Long userId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("매물을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 영속성 컨텍스트에서 컬렉션을 초기화하여 최신 상태 확인
        user.getFavoriteListings().size(); // 컬렉션 초기화
        
        // ID 기반으로 찜 여부 확인 (equals/hashCode 문제 방지)
        boolean isFavorite = user.getFavoriteListings().stream()
                .anyMatch(fav -> fav.getId().equals(listingId));
        
        if (isFavorite) {
            user.getFavoriteListings().removeIf(fav -> fav.getId().equals(listingId));
        } else {
            user.getFavoriteListings().add(listing);
        }
        
        // 변경사항을 즉시 DB에 반영
        userRepository.saveAndFlush(user);

        return !isFavorite; // 변경된 상태 반환
    }

    /**
     * 찜 목록 조회
     */
    public List<ListingDto> getFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return user.getFavoriteListings().stream()
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

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        return user.getFavoriteListings().stream()
                .anyMatch(fav -> fav.getId().equals(listingId));
    }
}

