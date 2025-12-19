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
}