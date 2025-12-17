package com.Minyou.MINYOU.repository;

import com.Minyou.MINYOU.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByListingIdAndBuyerId(Long listingId, Long buyerId);
    List<ChatRoom> findByOwnerIdOrBuyerId(Long ownerId, Long buyerId);
}
