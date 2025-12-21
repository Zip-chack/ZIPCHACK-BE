-- MySQL 테이블 생성 및 목데이터 삽입 스크립트
-- 주의: 외래키 제약조건을 고려하여 순서대로 실행해야 합니다.

CREATE DATABASE zip_chack
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE zip_chack;

-- 테이블 생성 (이미 존재하는 경우 DROP)
DROP TABLE IF EXISTS favorites;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS listings;
DROP TABLE IF EXISTS buildings;
DROP TABLE IF EXISTS users;

-- 1. Users 테이블 생성
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       nickname VARCHAR(255) NOT NULL,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Buildings 테이블 생성
CREATE TABLE buildings (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           road_address VARCHAR(255) NOT NULL,
                           lat DOUBLE NOT NULL,
                           lng DOUBLE NOT NULL,
                           built_year INT
);

-- 3. Listings 테이블 생성
CREATE TABLE listings (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          room_type VARCHAR(255) NOT NULL,
                          deposit INT NOT NULL,
                          monthly_rent INT NOT NULL,
                          maintenance_fee INT,
                          area_m2 DOUBLE,
                          floor INT NOT NULL,
                          image_url VARCHAR(255),
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          building_id BIGINT NOT NULL,
                          user_id BIGINT NOT NULL,
                          FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE,
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Reviews 테이블 생성
CREATE TABLE reviews (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         content TEXT NOT NULL,
                         rating_overall DOUBLE NOT NULL,
                         rating_noise DOUBLE,
                         rating_landlord DOUBLE,
                         rating_facility DOUBLE,
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                         user_id BIGINT NOT NULL,
                         listing_id BIGINT,
                         building_id BIGINT,
                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                         FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
                         FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
);

-- 5. Favorites 테이블 생성 (ManyToMany 관계)
CREATE TABLE favorites (
                           user_id BIGINT NOT NULL,
                           listing_id BIGINT NOT NULL,
                           PRIMARY KEY (user_id, listing_id),
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                           FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

-- 데이터 삽입 시작
-- 1. Users 테이블 데이터 삽입
-- 비밀번호는 BCrypt로 암호화된 값입니다 (원본: "password123")
INSERT INTO users (email, password, nickname, created_at) VALUES
                                                              ('user1@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김철수', NOW()),
                                                              ('user2@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '박영희', NOW()),
                                                              ('user3@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '이민수', NOW()),
                                                              ('user4@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '정수진', NOW()),
                                                              ('user5@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '최동현', NOW());

-- 2. Buildings 테이블 데이터 삽입
INSERT INTO buildings (name, road_address, lat, lng, built_year) VALUES
                                                                     ('행복빌라', '서울시 마포구 연남동 123-45', 37.5665, 126.9780, 2020),
                                                                     ('푸른하늘', '서울시 서대문구 신촌동 456-78', 37.5590, 126.9426, 2018),
                                                                     ('초록마을', '서울시 강서구 화곡동 789-12', 37.5412, 126.8497, 2015),
                                                                     ('햇살아파트', '서울시 강남구 역삼동 321-67', 37.5000, 127.0364, 2021),
                                                                     ('별빛빌딩', '서울시 송파구 잠실동 654-32', 37.5133, 127.1028, 2019),
                                                                     ('꿈의집', '서울시 종로구 명륜동 987-65', 37.5825, 126.9995, 2017),
                                                                     ('미래타워', '서울시 용산구 이태원동 147-25', 37.5347, 126.9947, 2022),
                                                                     ('하늘빌라', '서울시 영등포구 여의도동 258-14', 37.5219, 126.9242, 2016);

-- 3. Listings 테이블 데이터 삽입
INSERT INTO listings (title, room_type, deposit, monthly_rent, maintenance_fee, area_m2, floor, image_url, created_at, building_id, user_id) VALUES
                                                                                                                                                 ('신촌역 도보 5분 깔끔한 원룸', '원룸', 500, 50, 5, 18.5, 3, 'https://via.placeholder.com/400x300', NOW(), 2, 1),
                                                                                                                                                 ('연남동 조용한 1.5룸 전세', '1.5룸', 1000, 0, 10, 25.0, 2, 'https://via.placeholder.com/400x300', NOW(), 1, 2),
                                                                                                                                                 ('화곡동 신축 투룸 월세', '투룸', 1000, 70, 8, 35.5, 5, 'https://via.placeholder.com/400x300', NOW(), 3, 3),
                                                                                                                                                 ('강남역 근처 프리미엄 원룸', '원룸', 2000, 80, 15, 20.0, 10, 'https://via.placeholder.com/400x300', NOW(), 4, 4),
                                                                                                                                                 ('잠실역 도보 3분 1.5룸', '1.5룸', 1500, 60, 12, 28.0, 7, 'https://via.placeholder.com/400x300', NOW(), 5, 5),
                                                                                                                                                 ('명륜동 조용한 원룸', '원룸', 800, 55, 7, 19.5, 4, 'https://via.placeholder.com/400x300', NOW(), 6, 1),
                                                                                                                                                 ('이태원 근처 스튜디오', '원룸', 1200, 65, 10, 22.0, 6, 'https://via.placeholder.com/400x300', NOW(), 7, 2),
                                                                                                                                                 ('여의도 뷰 좋은 투룸', '투룸', 3000, 100, 20, 42.0, 15, 'https://via.placeholder.com/400x300', NOW(), 8, 3),
                                                                                                                                                 ('연남동 신축 원룸', '원룸', 600, 45, 5, 17.0, 2, 'https://via.placeholder.com/400x300', NOW(), 1, 4),
                                                                                                                                                 ('신촌역 근처 깔끔한 1.5룸', '1.5룸', 900, 58, 9, 26.5, 3, 'https://via.placeholder.com/400x300', NOW(), 2, 5);

-- 4. Reviews 테이블 데이터 삽입 (Listing 리뷰)
INSERT INTO reviews (title, content, rating_overall, rating_noise, rating_landlord, rating_facility, created_at, user_id, listing_id, building_id) VALUES
                                                                                                                                                       ('전반적으로 만족스러운 원룸', '역에서 가깝고 주변에 편의시설이 많아서 좋았습니다. 집주인분도 친절하시고 관리도 잘 해주세요.', 4.5, 4.0, 5.0, 4.0, NOW(), 2, 1, 2),
                                                                                                                                                       ('조용하고 깨끗해요', '채광이 좋고 환기도 잘 됩니다. 다만 가끔 위층 소음이 있어요.', 4.0, 3.0, 5.0, 4.0, NOW(), 3, 1, 2),
                                                                                                                                                       ('가성비 최고입니다', '이 가격에 이 정도 집은 정말 찾기 어려워요. 주변 교통도 편리하고 좋아요.', 4.8, 4.5, 4.5, 4.5, NOW(), 4, 2, 1),
                                                                                                                                                       ('강남역 근처라 교통 편리', '지하철역이 가까워서 출퇴근이 편해요. 다만 주변 음식점이 비싸요.', 4.2, 4.0, 4.0, 4.5, NOW(), 5, 4, 4),
                                                                                                                                                       ('잠실역 근처 최고의 위치', '롯데월드타워가 보여서 뷰가 정말 좋아요. 주변 상권도 발달되어 있어요.', 4.7, 4.5, 4.5, 5.0, NOW(), 1, 5, 5),
                                                                                                                                                       ('조용한 동네라 좋아요', '대학가 근처라 조용하고 안전해요. 다만 지하철역이 좀 멀어요.', 4.3, 5.0, 4.0, 3.5, NOW(), 2, 6, 6),
                                                                                                                                                       ('이태원 근처라 외국인 많아요', '다양한 문화를 경험할 수 있어서 좋아요. 다만 주말에 시끄러울 수 있어요.', 4.0, 3.5, 4.5, 4.0, NOW(), 3, 7, 7),
                                                                                                                                                       ('여의도 뷰 최고', '한강이 보여서 정말 좋아요. 투룸이라 공간도 넓어요.', 4.9, 4.5, 5.0, 5.0, NOW(), 4, 8, 8);

-- 5. Reviews 테이블 데이터 삽입 (Building 리뷰)
INSERT INTO reviews (title, content, rating_overall, rating_noise, rating_landlord, rating_facility, created_at, user_id, listing_id, building_id) VALUES
                                                                                                                                                       ('행복빌라 전체적으로 좋아요', '건물 관리가 잘 되고 있고, 주변 환경도 깨끗해요. 다만 엘리베이터가 없어서 불편할 수 있어요.', 4.3, 4.0, 4.5, 4.0, NOW(), 1, NULL, 1),
                                                                                                                                                       ('푸른하늘 건물 관리 우수', '관리사무소가 있어서 관리가 잘 되고 있어요. 주차 공간도 넉넉해요.', 4.6, 4.5, 4.5, 4.5, NOW(), 2, NULL, 2),
                                                                                                                                                       ('초록마을 가성비 좋은 건물', '건물이 오래되었지만 관리가 잘 되어 있어요. 주변 교통도 편리해요.', 4.2, 4.0, 4.0, 3.5, NOW(), 3, NULL, 3),
                                                                                                                                                       ('햇살아파트 신축이라 깨끗해요', '신축 건물이라 시설이 깨끗하고 좋아요. 다만 관리비가 좀 비싸요.', 4.7, 4.5, 4.5, 5.0, NOW(), 4, NULL, 4),
                                                                                                                                                       ('별빛빌딩 위치 최고', '잠실역이 가까워서 교통이 정말 편리해요. 주변 상권도 발달되어 있어요.', 4.8, 4.0, 4.5, 4.5, NOW(), 5, NULL, 5);

-- 6. Favorites 테이블 데이터 삽입 (찜하기)
INSERT INTO favorites (user_id, listing_id) VALUES
                                                (1, 1),
                                                (1, 3),
                                                (1, 5),
                                                (2, 2),
                                                (2, 4),
                                                (3, 1),
                                                (3, 6),
                                                (4, 5),
                                                (4, 8),
                                                (5, 2),
                                                (5, 7);

-- 7. chat_room 테이블 데이터 삽입 (채팅방 생성)
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

-- 8. chat_message 테이블 생성 (채팅 메시지)
CREATE TABLE chat_message (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              room_id BIGINT NOT NULL,
                              sender_id BIGINT NOT NULL,
                              content TEXT NOT NULL,
                              read_by_owner BOOLEAN NOT NULL DEFAULT FALSE,
                              read_by_buyer BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at DATETIME(6) NOT NULL,
                              updated_at DATETIME(6) NOT NULL, -- Inherited from BaseTimeEntity, but for DB schema just added
                              CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES chat_room(id)
);

-- 추가 인덱스
CREATE INDEX idx_chat_room_listing_id ON chat_room (listing_id);
CREATE INDEX idx_chat_room_owner_id ON chat_room (owner_id);
CREATE INDEX idx_chat_room_buyer_id ON chat_room (buyer_id);
CREATE INDEX idx_chat_message_room_id ON chat_message (room_id);
CREATE INDEX idx_chat_message_sender_id ON chat_message (sender_id);
