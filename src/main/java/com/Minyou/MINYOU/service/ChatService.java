package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.*;
import com.Minyou.MINYOU.entity.ChatRoomStatus;
import com.Minyou.MINYOU.entity.ChatRoom;
import com.Minyou.MINYOU.entity.ChatMessage;
import com.Minyou.MINYOU.entity.Listing;
import com.Minyou.MINYOU.repository.ChatRoomRepository;
import com.Minyou.MINYOU.repository.ChatMessageRepository;
import com.Minyou.MINYOU.repository.ListingRepository;
import com.Minyou.MINYOU.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;   // Changed: Injected UserRepository
    private final SimpMessageSendingOperations messagingTemplate;

    @Transactional
    public ChatRoomResponse createOrGetRoom(Long listingId, Long buyerId) {
        return chatRoomRepository.findByListingIdAndBuyerId(listingId, buyerId)
                .map(room -> {
                    Long opponentId = room.getOwnerId().equals(buyerId) ? room.getBuyerId() : room.getOwnerId();
                    return new ChatRoomResponse(room.getId(), opponentId, room.getStatus());
                })
                .orElseGet(() -> {
                    Listing listing = listingRepository.findById(listingId)
                            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
                    if (listing.getUser().getId().equals(buyerId)) {
                        throw new IllegalArgumentException("Owner cannot start a chat with themselves.");
                    }
                    ChatRoom newRoom = ChatRoom.builder()
                            .listingId(listingId)
                            .ownerId(listing.getUser().getId())
                            .buyerId(buyerId)
                            .build();
                    chatRoomRepository.save(newRoom);
                    return new ChatRoomResponse(newRoom.getId(), newRoom.getOwnerId(), newRoom.getStatus());
                });
    }

    public List<MyChatRoomResponse> getMyChatRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByOwnerIdOrBuyerId(userId, userId);
        return rooms.stream().map(room -> {
            Long opponentId = room.getOwnerId().equals(userId) ? room.getBuyerId() : room.getOwnerId();
            String lastMessage = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId())
                    .stream().reduce((first, second) -> second).map(ChatMessage::getContent).orElse("No messages yet.");
            return MyChatRoomResponse.builder()
                    .roomId(room.getId())
                    .opponentId(opponentId)
                    .lastMessage(lastMessage)
                    .status(room.getStatus())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatMessages(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found."));
        if (!room.getOwnerId().equals(userId) && !room.getBuyerId().equals(userId)) {
            throw new SecurityException("You do not have permission to view this chat.");
        }
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long saveMessageAndGetOpponentId(Long roomId, ChatMessageDto dto) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found."));

        if (room.getStatus() == ChatRoomStatus.COMPLETED) {
            throw new IllegalStateException("This chat room is already completed.");
        }

        if (room.getStatus() == ChatRoomStatus.WAITING) {
            if (chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId).isEmpty()) {
                room.updateStatus(ChatRoomStatus.NEGOTIATING);
            }
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderId(dto.getSenderId())
                .content(dto.getContent())
                .build();
        chatMessageRepository.save(message);

        return room.getOwnerId().equals(dto.getSenderId()) ? room.getBuyerId() : room.getOwnerId();
    }

    @Transactional
    public void completeChat(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found."));

        if (!room.getOwnerId().equals(userId) && !room.getBuyerId().equals(userId)) {
            throw new SecurityException("You do not have permission to complete this chat.");
        }

        if (room.getStatus() == ChatRoomStatus.COMPLETED) {
            return;
        }

        room.updateStatus(ChatRoomStatus.COMPLETED);
        sendSystemMessage(room, "거래가 완료되었습니다. 이 채팅방은 종료됩니다.");
    }

    private void sendSystemMessage(ChatRoom room, String content) {
        ChatMessageDto systemMessage = new ChatMessageDto(0L, content);
        messagingTemplate.convertAndSend("/queue/chat/" + room.getId() + "/user/" + room.getOwnerId(), systemMessage);
        messagingTemplate.convertAndSend("/queue/chat/" + room.getId() + "/user/" + room.getBuyerId(), systemMessage);
    }
}