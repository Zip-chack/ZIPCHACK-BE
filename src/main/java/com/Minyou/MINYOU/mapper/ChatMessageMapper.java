package com.Minyou.MINYOU.mapper;

import com.Minyou.MINYOU.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    List<ChatMessage> findAll();
    ChatMessage findById(Long id);
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(@Param("roomId") Long roomId);
    void deleteByChatRoomId(@Param("roomId") Long roomId);
    long countUnreadMessagesByUserId(@Param("userId") Long userId);
    long countUnreadMessagesByChatRoomIdAndUserId(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
    void insert(ChatMessage chatMessage);
    void update(ChatMessage chatMessage);
    void delete(Long id);
}

