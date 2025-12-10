# ZIP-Chack Backend

원룸 리뷰 플랫폼 백엔드 API 서버

## 📋 프로젝트 소개

ZIP-Chack 백엔드는 실거주자 리뷰 기반 원룸 정보 플랫폼의 REST API 서버입니다. Spring Boot를 기반으로 구현되었으며, 카카오맵 API와 공공데이터 API를 연동하여 지도 기반 검색 및 실거래가 정보를 제공합니다.

## 🛠 기술 스택

- **Java 17**
- **Spring Boot 3.5.9**
- **Spring Data JPA**
- **Spring Security**
- **MySQL**
- **Spring WebFlux** (WebClient 사용)
- **Lombok**
- **Dotenv Java**

## 📁 프로젝트 구조

```
ZIPCHACK-BE/
├── src/
│   ├── main/
│   │   ├── java/com/Minyou/MINYOU/
│   │   │   ├── config/          # 설정 클래스
│   │   │   │   ├── JpaConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/      # REST 컨트롤러
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── BuildingController.java
│   │   │   │   ├── HomeController.java
│   │   │   │   ├── ListingController.java
│   │   │   │   ├── ReviewController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/             # 데이터 전송 객체
│   │   │   ├── entity/          # JPA 엔티티
│   │   │   ├── repository/      # 데이터 접근 계층
│   │   │   ├── service/         # 비즈니스 로직
│   │   │   └── interceptor/    # 인터셉터
│   │   └── resources/
│   │       ├── application.properties
│   │       └── database/
│   └── test/
├── pom.xml
└── Dockerfile
```

## 🚀 실행 방법

### 사전 요구사항

- Java 17 이상
- Maven 3.6 이상
- MySQL 8.0 이상

### 로컬 실행

```bash
# 프로젝트 디렉토리로 이동
cd ZIPCHACK-BE

# Maven Wrapper를 사용한 실행
./mvnw spring-boot:run

# 또는 IntelliJ IDEA에서 MinyouApplication.java 실행
```

### Docker 실행

```bash
# Docker Compose 사용
docker-compose up -d

# 또는 Dockerfile로 직접 빌드
docker build -t zipchack-be .
docker run -p 8080:8080 zipchack-be
```

## ⚙️ 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성하고 다음 환경 변수를 설정하세요:

```env
# Database Configuration
DB_URL=jdbc:mysql://localhost:3307/zip_chack?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password

# Application Configuration
APP_NAME=MINYOU
SERVER_PORT=8080

# Kakao Map API (REST API Key)
KAKAO_MAP_REST_API_KEY=your_kakao_rest_api_key

# Public Data API (선택사항)
KDATA_KEY=your_kdata_api_key
```

### 환경 변수 설명

- `DB_URL`: MySQL 데이터베이스 연결 URL
- `DB_USERNAME`: 데이터베이스 사용자명
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `APP_NAME`: 애플리케이션 이름 (기본값: MINYOU)
- `SERVER_PORT`: 서버 포트 (기본값: 8080)
- `KAKAO_MAP_REST_API_KEY`: 카카오맵 REST API 키 (필수)
- `KDATA_KEY`: 공공데이터 API 키 (선택사항)

## 📡 API 엔드포인트

### 인증 (Auth)

- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/logout` - 로그아웃
- `GET /api/auth/me` - 현재 사용자 정보

### 건물 (Building)

- `GET /api/buildings` - 건물 목록 조회
- `GET /api/buildings/{id}` - 건물 상세 조회
- `GET /api/buildings/search?q={query}` - 건물 검색
- `POST /api/buildings` - 건물 등록

### 매물 (Listing)

- `GET /api/listings` - 매물 목록 조회
- `GET /api/listings/{id}` - 매물 상세 조회
- `POST /api/listings` - 매물 등록
- `PUT /api/listings/{id}` - 매물 수정
- `DELETE /api/listings/{id}` - 매물 삭제
- `POST /api/listings/{id}/favorite` - 찜하기
- `GET /api/listings/favorites` - 찜 목록 조회

### 리뷰 (Review)

- `GET /api/listings/{id}/reviews` - 매물 리뷰 조회
- `POST /api/listings/{id}/reviews` - 매물 리뷰 작성
- `GET /api/buildings/{id}/reviews` - 건물 리뷰 조회
- `POST /api/buildings/{id}/reviews` - 건물 리뷰 작성
- `PUT /api/reviews/{id}` - 리뷰 수정
- `DELETE /api/reviews/{id}` - 리뷰 삭제

### 헬스체크

- `GET /health` - 서버 상태 확인

## 🔑 API 키 발급

### 카카오맵 API

1. [카카오 개발자 콘솔](https://developers.kakao.com) 접속
2. 애플리케이션 생성
3. **REST API 키** 발급 (백엔드용)

### 공공데이터 API (선택사항)

1. [공공데이터포털](https://www.data.go.kr) 접속
2. "국토교통부\_아파트 전월세 실거래가" 검색
3. API 키 발급 및 인코딩

## 🗄 데이터베이스 설정

### MySQL 데이터베이스 생성

```sql
CREATE DATABASE zip_chack CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 스키마 초기화

JPA의 `spring.jpa.hibernate.ddl-auto=update` 설정으로 자동으로 테이블이 생성됩니다.

또는 `src/main/resources/database/zipchack_db.sql` 파일을 사용하여 수동으로 스키마를 생성할 수 있습니다.

## 📝 주요 기능

- ✅ 사용자 인증 및 권한 관리
- ✅ 건물 및 매물 CRUD
- ✅ 리뷰 작성 및 관리
- ✅ 찜 목록 관리
- ✅ 카카오맵 API 연동 (주소 검색, 키워드 검색, 좌표 변환)
- ✅ 공공데이터 API 연동 (아파트 전월세 실거래가)

## 🔧 빌드

```bash
# 프로덕션 빌드
.\mvnw spring-boot:run

# 빌드된 JAR 파일 실행
java -jar target/MINYOU-0.0.1-SNAPSHOT.jar
```

## 📄 라이선스

싸피 관통 프로젝트
