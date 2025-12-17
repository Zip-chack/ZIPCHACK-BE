-- SQL for creating chat_room table
CREATE TABLE chat_room (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           listing_id BIGINT NOT NULL,
                           owner_id BIGINT NOT NULL,
                           buyer_id BIGINT NOT NULL,
                           status VARCHAR(20) NOT NULL, -- WAITING, NEGOTIATING, COMPLETED
                           created_at DATETIME(6) NOT NULL,
                           updated_at DATETIME(6) NOT NULL,
                           CONSTRAINT uk_chat_room_listing_buyer UNIQUE (listing_id, buyer_id)
);

-- SQL for creating chat_message table
CREATE TABLE chat_message (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              room_id BIGINT NOT NULL,
                              sender_id BIGINT NOT NULL,
                              content TEXT NOT NULL,
                              created_at DATETIME(6) NOT NULL,
                              updated_at DATETIME(6) NOT NULL, -- Inherited from BaseTimeEntity, but for DB schema just added
                              CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES chat_room(id)
);

-- Optional: Add indexes for performance
CREATE INDEX idx_chat_room_listing_id ON chat_room (listing_id);
CREATE INDEX idx_chat_room_owner_id ON chat_room (owner_id);
CREATE INDEX idx_chat_room_buyer_id ON chat_room (buyer_id);
CREATE INDEX idx_chat_message_room_id ON chat_message (room_id);
CREATE INDEX idx_chat_message_sender_id ON chat_message (sender_id);
