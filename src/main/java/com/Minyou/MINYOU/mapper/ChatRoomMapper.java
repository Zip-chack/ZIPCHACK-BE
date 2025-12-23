package com.Minyou.MINYOU.mapper;

import com.Minyou.MINYOU.entity.ChatRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ChatRoomMapper {
    List<ChatRoom> findAll();
    ChatRoom findById(Long id);
    Optional<ChatRoom> findByListingIdAndBuyerId(@Param("listingId") Long listingId, @Param("buyerId") Long buyerId);
    List<ChatRoom> findByOwnerIdOrBuyerId(@Param("ownerId") Long ownerId, @Param("buyerId") Long buyerId);
    void insert(ChatRoom chatRoom);
    void update(ChatRoom chatRoom);
    void delete(Long id);
}

