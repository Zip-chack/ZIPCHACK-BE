package com.Minyou.MINYOU.service;

import com.Minyou.MINYOU.dto.*;
import com.Minyou.MINYOU.entity.ChatRoomStatus;
import com.Minyou.MINYOU.entity.ChatRoom;
import com.Minyou.MINYOU.entity.ChatMessage;
import com.Minyou.MINYOU.entity.Listing;
import com.Minyou.MINYOU.entity.ListingStatus;
import com.Minyou.MINYOU.entity.User;
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
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 매물 ID와 구매자 ID 기준으로 채팅방을 조회하거나 없으면 새로 생성한다.
     */
    @Transactional
    public ChatRoomResponse createOrGetRoom(Long listingId, Long loginUserId) {
        return chatRoomRepository.findByListingIdAndBuyerId(listingId, loginUserId)
                .map(room -> {
                    // 기존 채팅방이 있을 경우,
                    // 로그인 사용자가 owner인지 buyer인지에 따라 상대방을 결정
                    Long targetUserId = room.getOwnerId().equals(loginUserId) ? room.getBuyerId() : room.getOwnerId();
                    String targetUserNickname = userRepository.findById(targetUserId)
                            .map(com.Minyou.MINYOU.entity.User::getNickname)
                            .orElse("Unknown User");
                    return new ChatRoomResponse(room.getId(), targetUserId, targetUserNickname, room.getStatus());
                })
                .orElseGet(() -> {
                    // 채팅방이 없는 경우 새로 생성
                    Listing listing = listingRepository.findById(listingId)
                            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
                    if (listing.getUser().getId().equals(loginUserId)) {
                        throw new IllegalArgumentException("Owner cannot start a chat with themselves.");
                    }

                    // 채팅 상대방은 매물 등록자
                    User targetUser = listing.getUser();

                    ChatRoom newRoom = ChatRoom.builder()
                            .listingId(listingId)
                            .ownerId(targetUser.getId()) // 매물 소유자
                            .buyerId(loginUserId)        // 채팅을 시작한 사용자
                            .build();
                    chatRoomRepository.save(newRoom);
                    
                    return new ChatRoomResponse(newRoom.getId(), targetUser.getId(), targetUser.getNickname(), newRoom.getStatus());
                });
    }

    /**
     * 사용자가 참여 중인 모든 채팅방 목록과 마지막 메시지를 조회한다.
     */
    public List<MyChatRoomResponse> getMyChatRooms(Long loginUserId) {
        List<ChatRoom> rooms = chatRoomRepository.findByOwnerIdOrBuyerId(loginUserId, loginUserId);
        return rooms.stream().map(room -> {
            Long targetUserId = room.getOwnerId().equals(loginUserId) ? room.getBuyerId() : room.getOwnerId();
            // 로그인 사용자 기준으로 상대방 정보 계산
            String targetUserNickname = userRepository.findById(targetUserId)
                    .map(com.Minyou.MINYOU.entity.User::getNickname)
                    .orElse("Unknown User");

            String lastMessage = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId())
                    .stream().reduce((first, second) -> second).map(ChatMessage::getContent).orElse("No messages yet.");
            
            return MyChatRoomResponse.builder()
                    .roomId(room.getId())
                    .targetUserId(targetUserId)
                    .targetUserNickname(targetUserNickname)
                    .lastMessage(lastMessage)
                    .status(room.getStatus())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 특정 채팅방의 전체 메시지를 시간순으로 조회한다.
     */
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

    /**
     * 특정 채팅방의 상세 정보를 조회한다.
     */
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoomById(Long roomId, Long loginUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found."));
        if (!room.getOwnerId().equals(loginUserId) && !room.getBuyerId().equals(loginUserId)) {
            throw new SecurityException("You do not have permission to access this chat room.");
        }
        Long targetUserId = room.getOwnerId().equals(loginUserId) ? room.getBuyerId() : room.getOwnerId();
        String targetUserNickname = userRepository.findById(targetUserId)
                .map(com.Minyou.MINYOU.entity.User::getNickname)
                .orElse("Unknown User");
        return new ChatRoomResponse(room.getId(), targetUserId, targetUserNickname, room.getStatus());
    }

    /**
     * 채팅 메시지를 저장하고 저장된 메시지 엔티티를 반환한다.
     */
    @Transactional
    public ChatMessage saveMessage(Long roomId, ChatMessageDto dto) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found."));

        if (room.getStatus() == ChatRoomStatus.COMPLETED) {
            throw new IllegalStateException("This chat room is already completed.");
        }

        // 첫 메시지 전송 시 상태를 NEGOTIATING으로 변경
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
        
        return chatMessageRepository.save(message);
    }

    /**
     * 채팅방을 거래 완료 상태로 변경하고 시스템 메시지를 전송한다.
     */
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

        // 해당 채팅방과 연결된 매물의 상태도 '거래 완료'로 변경
        listingRepository.findById(room.getListingId()).ifPresent(listing -> {
            listing.setStatus(ListingStatus.COMPLETED);
        });

        sendSystemMessage(room, "거래가 완료되었습니다. 이 채팅방은 종료됩니다.");
    }

    /**
     * 채팅방 참여자 모두에게 시스템 메시지를 전송한다.
     */
    private void sendSystemMessage(ChatRoom room, String content) {
        ChatMessageDto systemMessage = new ChatMessageDto(0L, content, null);
        messagingTemplate.convertAndSend("/queue/chat/" + room.getId() + "/user/" + room.getOwnerId(), systemMessage);
        messagingTemplate.convertAndSend("/queue/chat/" + room.getId() + "/user/" + room.getBuyerId(), systemMessage);
    }
}