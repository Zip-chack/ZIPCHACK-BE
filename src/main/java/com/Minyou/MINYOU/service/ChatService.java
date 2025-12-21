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
    public ChatRoomResponse createOrGetRoom(Long listingId, Long loginUserId) {
        return chatRoomRepository.findByListingIdAndBuyerId(listingId, loginUserId)
                .map(room -> {
                    Long targetUserId = room.getOwnerId().equals(loginUserId) ? room.getBuyerId() : room.getOwnerId();
                    String targetUserNickname = userRepository.findById(targetUserId)
                            .map(com.Minyou.MINYOU.entity.User::getNickname)
                            .orElse("Unknown User");
                    boolean amIOwner = room.getOwnerId().equals(loginUserId);
                    return new ChatRoomResponse(room.getId(), targetUserId, targetUserNickname, room.getStatus(), amIOwner);
                })
                .orElseGet(() -> {
                    Listing listing = listingRepository.findById(listingId)
                            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
                    
                    if (listing.getStatus() == ListingStatus.COMPLETED) {
                        throw new IllegalStateException("이미 거래가 완료된 매물입니다.");
                    }

                    if (listing.getUser().getId().equals(loginUserId)) {
                        throw new IllegalArgumentException("Owner cannot start a chat with themselves.");
                    }
                    
                    User targetUser = listing.getUser();

                    ChatRoom newRoom = ChatRoom.builder()
                            .listingId(listingId)
                            .ownerId(targetUser.getId()) // Listing owner is the owner of the chat
                            .buyerId(loginUserId)         // Initiating user is the buyer of the chat
                            .build();
                    chatRoomRepository.save(newRoom);
                    
                    boolean amIOwner = false;
                    return new ChatRoomResponse(newRoom.getId(), targetUser.getId(), targetUser.getNickname(), newRoom.getStatus(), amIOwner);
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
     * 채팅방 입장 시 모든 메시지를 읽음 처리한다.
     */
    @Transactional
    public List<ChatMessageResponse> getChatMessages(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found."));
        if (!room.getOwnerId().equals(userId) && !room.getBuyerId().equals(userId)) {
            throw new SecurityException("You do not have permission to view this chat.");
        }
        
        // 채팅방 입장 시 모든 메시지를 읽음 처리
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId);
        messages.forEach(message -> {
            if (room.getOwnerId().equals(userId) && !message.getReadByOwner()) {
                message.markAsReadByOwner();
            } else if (room.getBuyerId().equals(userId) && !message.getReadByBuyer()) {
                message.markAsReadByBuyer();
            }
        });
        
        return messages.stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 읽지 않은 메시지 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(Long userId) {
        return chatMessageRepository.countUnreadMessagesByUserId(userId);
    }

    /**
     * 특정 채팅방의 상세 정보를 조회한다.
     */
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
        boolean amIOwner = room.getOwnerId().equals(loginUserId);
        return new ChatRoomResponse(room.getId(), targetUserId, targetUserNickname, room.getStatus(), amIOwner);
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
                // 매물 상태를 예약중으로 변경
                listingRepository.findById(room.getListingId()).ifPresent(l -> {
                    if (l.getStatus() == ListingStatus.AVAILABLE) {
                        l.setStatus(ListingStatus.RESERVED);
                        listingRepository.saveAndFlush(l);
                    }
                });
            }
        }

        // 새 메시지는 기본적으로 읽지 않은 상태로 생성
        // 보낸 사람의 메시지는 즉시 읽음 처리 (자신이 보낸 메시지는 읽은 것으로 간주)
        boolean isOwner = room.getOwnerId().equals(dto.getSenderId());
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderId(dto.getSenderId())
                .content(dto.getContent())
                .readByOwner(isOwner) // 보낸 사람이 owner면 owner는 읽음, buyer는 읽지 않음
                .readByBuyer(!isOwner) // 보낸 사람이 buyer면 buyer는 읽음, owner는 읽지 않음
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

        // 매물 등록자(Owner)만 거래 완료 가능
        if (!room.getOwnerId().equals(userId)) {
            throw new SecurityException("오직 매물 등록자만 거래를 완료할 수 있습니다.");
        }

        if (room.getStatus() == ChatRoomStatus.COMPLETED) {
            System.out.println("Chat room " + roomId + " is already COMPLETED.");
            return;
        }

        System.out.println("Updating chat room " + roomId + " to COMPLETED.");
        room.updateStatus(ChatRoomStatus.COMPLETED);
        chatRoomRepository.saveAndFlush(room);

        // 해당 채팅방과 연결된 매물의 상태도 '거래 완료'로 변경
        listingRepository.findById(room.getListingId()).ifPresent(listing -> {
            System.out.println("Updating listing " + listing.getId() + " to COMPLETED.");
            listing.setStatus(ListingStatus.COMPLETED);
            listingRepository.saveAndFlush(listing);
        });

        sendSystemMessage(room, "거래가 완료되었습니다. 이 채팅방은 종료됩니다.");
    }

    /**
     * 채팅방과 관련된 모든 메시지를 삭제하고 채팅방을 삭제한다.
     */
    @Transactional
    public void deleteChatRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found."));

        if (!room.getOwnerId().equals(userId) && !room.getBuyerId().equals(userId)) {
            throw new SecurityException("You do not have permission to delete this chat room.");
        }

        // 1. 채팅 메시지 삭제
        chatMessageRepository.deleteByChatRoomId(roomId);

        // 2. 채팅방 삭제
        chatRoomRepository.deleteById(roomId);
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