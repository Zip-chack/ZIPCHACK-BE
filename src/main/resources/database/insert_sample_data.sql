-- 샘플 데이터 삽입 스크립트 (UTF-8 지원)
-- 실행 방법: docker exec -i minyou-mysql mysql -u root -prootpassword --default-character-set=utf8mb4 zip_chack < insert_sample_data.sql
-- 주의: 백엔드를 먼저 실행하여 테이블이 생성된 후 실행하세요.

USE zip_chack;

-- 문자셋 설정
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- 기존 데이터 삭제 (테이블이 존재하는 경우에만)
SET FOREIGN_KEY_CHECKS = 0;

-- Users 샘플 데이터 (비밀번호: password123 - BCrypt 해시)
INSERT INTO users (id, email, nickname, password, name, username, email_verified, created_at) VALUES
(1, 'user1@example.com', '홍길동', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '홍길동', 'honggildong', 1, NOW()),
(2, 'user2@example.com', '김철수', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김철수', 'kimcheolsu', 1, NOW()),
(3, 'user3@example.com', '이영희', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '이영희', 'leeyounghee', 1, NOW())
ON DUPLICATE KEY UPDATE email=email;

-- Buildings 샘플 데이터 (서울 강남구, 홍대, 이태원 지역)
INSERT INTO buildings (id, name, road_address, lat, lng, built_year) VALUES
(1, '강남 래미안 아파트', '서울특별시 강남구 테헤란로 123', 37.4979, 127.0276, 2015),
(2, '홍대 더샵 오피스텔', '서울특별시 마포구 홍익로 456', 37.5563, 126.9230, 2020),
(3, '이태원 힐스테이트', '서울특별시 용산구 이태원로 789', 37.5347, 126.9946, 2018),
(4, '신촌 스마트빌', '서울특별시 서대문구 연세로 321', 37.5598, 126.9373, 2019),
(5, '건대 프리미엄 원룸', '서울특별시 광진구 능동로 654', 37.5407, 127.0692, 2021)
ON DUPLICATE KEY UPDATE name=name;

-- Listings 샘플 데이터
INSERT INTO listings (id, building_id, user_id, title, room_type, deposit, monthly_rent, maintenance_fee, area_m2, floor, image_url, created_at) VALUES
(1, 1, 1, '강남역 도보 5분 원룸', '원룸', 10000000, 500000, 50000, 25.5, 5, 'https://via.placeholder.com/400x300', NOW()),
(2, 1, 1, '강남역 투룸 전세', '투룸', 200000000, 0, 0, 45.0, 10, 'https://via.placeholder.com/400x300', NOW()),
(3, 2, 2, '홍대입구역 신축 원룸', '원룸', 5000000, 600000, 60000, 20.0, 3, 'https://via.placeholder.com/400x300', NOW()),
(4, 2, 2, '홍대 프리미엄 오피스텔', '오피스텔', 15000000, 800000, 70000, 35.0, 8, 'https://via.placeholder.com/400x300', NOW()),
(5, 3, 3, '이태원 전망 좋은 원룸', '원룸', 8000000, 550000, 55000, 22.0, 12, 'https://via.placeholder.com/400x300', NOW()),
(6, 4, 1, '신촌역 1분 거리 원룸', '원룸', 3000000, 400000, 40000, 18.0, 2, 'https://via.placeholder.com/400x300', NOW()),
(7, 5, 2, '건대입구 신축 원룸', '원룸', 7000000, 650000, 60000, 24.0, 7, 'https://via.placeholder.com/400x300', NOW()),
(8, 5, 3, '건대 투룸 월세', '투룸', 10000000, 900000, 80000, 42.0, 15, 'https://via.placeholder.com/400x300', NOW())
ON DUPLICATE KEY UPDATE title=title;

-- Reviews 샘플 데이터
INSERT INTO reviews (id, user_id, building_id, listing_id, title, content, rating_overall, rating_facility, rating_landlord, rating_noise, created_at) VALUES
(1, 1, 1, 1, '강남역 접근성 최고', '교통이 정말 편리하고 주변에 편의시설이 많아요. 다만 소음이 조금 있는 편입니다.', 4.0, 4.5, 4.0, 3.0, NOW()),
(2, 2, 2, 3, '홍대 신축 건물 추천', '신축이라 깨끗하고 관리가 잘 되어있어요. 집주인분도 친절하시고요.', 4.5, 5.0, 4.5, 4.5, NOW()),
(3, 3, 3, 5, '이태원 전망 좋아요', '전망이 정말 좋고 조용한 편이에요. 다만 계약 시 주의하세요.', 3.5, 4.0, 3.0, 4.5, NOW()),
(4, 1, 4, 6, '신촌역 가까워서 좋아요', '대학가 근처라 생활하기 편리해요. 가격 대비 만족합니다.', 4.0, 3.5, 4.0, 3.5, NOW()),
(5, 2, 5, 7, '건대 신축 원룸', '신축이라 깨끗하고 시설이 좋아요. 주변 상권도 발달되어 있어요.', 4.5, 4.5, 4.5, 4.0, NOW())
ON DUPLICATE KEY UPDATE title=title;

-- Favorites 샘플 데이터
INSERT INTO favorites (user_id, listing_id) VALUES
(1, 3),
(1, 5),
(2, 1),
(2, 6),
(3, 2),
(3, 7)
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 데이터 확인
SELECT 'Users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'Buildings', COUNT(*) FROM buildings
UNION ALL
SELECT 'Listings', COUNT(*) FROM listings
UNION ALL
SELECT 'Reviews', COUNT(*) FROM reviews
UNION ALL
SELECT 'Favorites', COUNT(*) FROM favorites;
