package com.Minyou.MINYOU.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_room",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_listing_buyer",
                        columnNames = {"listing_id", "buyer_id"}
                )
        }
)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status;

    @Builder
    public ChatRoom(Long listingId, Long ownerId, Long buyerId) {
        this.listingId = listingId;
        this.ownerId = ownerId;
        this.buyerId = buyerId;
        this.status = ChatRoomStatus.WAITING;
    }

    public void updateStatus(ChatRoomStatus status) {
        this.status = status;
    }
}