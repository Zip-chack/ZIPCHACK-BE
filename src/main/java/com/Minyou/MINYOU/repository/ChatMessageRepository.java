package com.Minyou.MINYOU.repository;

import com.Minyou.MINYOU.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long roomId);

    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId")
    void deleteByChatRoomId(@Param("roomId") Long roomId);

    /**
     * 사용자가 읽지 않은 메시지 개수 조회
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
           "JOIN cm.chatRoom cr " +
           "WHERE (cr.ownerId = :userId OR cr.buyerId = :userId) " +
           "AND cm.senderId != :userId " +
           "AND ((cr.ownerId = :userId AND cm.readByOwner = false) OR " +
           "     (cr.buyerId = :userId AND cm.readByBuyer = false))")
    long countUnreadMessagesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :chatRoomId " +
           "AND cm.senderId != :userId " +
           "AND ((cm.chatRoom.ownerId = :userId AND cm.readByOwner = false) OR " +
           "     (cm.chatRoom.buyerId = :userId AND cm.readByBuyer = false))")
    long countUnreadMessagesByChatRoomIdAndUserId(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}