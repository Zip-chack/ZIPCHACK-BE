package com.Minyou.MINYOU.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom chatRoom;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "read_by_owner", nullable = false)
    private Boolean readByOwner = false;

    @Column(name = "read_by_buyer", nullable = false)
    private Boolean readByBuyer = false;

    @Builder
    public ChatMessage(ChatRoom chatRoom, Long senderId, String content, Boolean readByOwner, Boolean readByBuyer) {
        this.chatRoom = chatRoom;
        this.senderId = senderId;
        this.content = content;
        this.readByOwner = readByOwner != null ? readByOwner : false;
        this.readByBuyer = readByBuyer != null ? readByBuyer : false;
    }

    public void markAsReadByOwner() {
        this.readByOwner = true;
    }

    public void markAsReadByBuyer() {
        this.readByBuyer = true;
    }

    public boolean isReadByUser(Long userId, Long ownerId) {
        if (userId.equals(ownerId)) {
            return readByOwner;
        } else {
            return readByBuyer;
        }
    }
}