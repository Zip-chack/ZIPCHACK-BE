# ZIP-Chack Backend

**꼼꼼하게 따져보는 원룸 리뷰 & 정보 플랫폼, ZIP-CHACK**

ZIP-Chack 백엔드는 실거주자 리뷰 기반 원룸 정보 플랫폼의 REST API 서버입니다. Spring Boot를 기반으로 구현되었으며, 카카오맵 API 연동을 통한 지도 기반 검색과 WebSocket을 이용한 실시간 채팅을 지원합니다.

## 🛠 기술 스택

- **Language:** Java 17
- **Framework:** Spring Boot 3.5.9
- **Database:** MySQL
- **ORM:** Spring Data JPA
- **Security:** Spring Security (JWT, CORS)
- **Communication:** Spring WebFlux (WebClient), WebSocket (STOMP)
- **External APIs:** Kakao Map, AWS S3
- **Documentation:** Swagger (SpringDoc OpenAPI 2.8.3)
- **Tools:** Lombok, Dotenv Java

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/com/Minyou/MINYOU/
│   │   ├── config/          # 설정 (Security, WebSocket, Swagger 등)
│   │   ├── controller/      # REST API 컨트롤러
│   │   ├── dto/             # 데이터 전송 객체 (Request/Response)
│   │   ├── entity/          # JPA 엔티티 (DB 테이블 매핑)
│   │   ├── repository/      # Repository 인터페이스
│   │   ├── service/         # 비즈니스 로직
│   │   └── security/        # JWT 인증 필터 및 핸들러
│   └── resources/
│       ├── application.properties
│       └── database/        # DB 스키마 및 초기 데이터
└── test/                    # JUnit 테스트 코드
```

## 🚀 실행 방법

### 사전 요구사항

- Java 17 이상
- Maven 3.6 이상
- MySQL 8.0 이상

### 로컬 실행

1. **데이터베이스 설정**
   MySQL에 `zip_chack` 스키마를 생성합니다.

2. **환경 변수 설정**
   프로젝트 루트에 `.env` 파일을 생성하고 설정을 입력합니다. (하단 참조)

3. **서버 실행**
   ```bash
   cd ZIPCHACK-BE
   ./mvnw clean spring-boot:run
   ```

### Docker 실행

```bash
# 빌드 및 실행
docker-compose up -d --build
```

## 📖 API 명세서 (Swagger)

서버 실행 후 아래 주소에서 전체 API 명세 확인 및 테스트가 가능합니다.

*   **주소:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

> **인증 테스트 방법:**
> 1. Auth API를 통해 로그인을 진행하고 `accessToken`을 복사합니다.
> 2. Swagger 우측 상단의 **Authorize** 버튼을 누릅니다.
> 3. 입력창에 `Bearer {복사한_토큰}` 형식을 입력하고 Authorize를 클릭합니다. (예: `Bearer eyJ...`)

## 📡 주요 API 기능

### 1. 🏠 매물 및 건물 (Listing & Building)
- **매물 관리**: 매물 등록, 수정, 삭제, 상태 변경(판매중/예약중/완료)
- **건물 정보**: 건물 상세 조회 및 매물 연동
- **검색/필터**: 최신순 정렬, 키워드 검색
- **찜하기**: 관심 매물 즐겨찾기 기능

### 2. 🗺️ 지도 및 상권 분석 (Map & Commerce)
- **카카오맵 연동**: 주소-좌표 변환, 키워드 검색, 영역 내 건물 조회
- **AI 상권 분석**: Python AI 서버와 연동하여 해당 위치의 상권 분석 리포트를 생성하여 반환합니다.

### 3. 💬 실시간 채팅 (Chat)
- **WebSocket + STOMP**: 실시간 메시지 전송 및 수신
- **채팅방 관리**: 매물 기준 채팅방 생성, 내 채팅 목록 조회
- **상태 동기화**: 채팅방 내 거래 완료 시 매물 상태 자동 업데이트
- **읽음 확인**: 메시지 읽음 처리 및 안 읽은 메시지 카운트

### 4. 📝 리뷰 시스템 (Review)
- **다중 리뷰**: 건물 리뷰와 매물 리뷰를 분리하여 작성 가능
- **평점**: 별점 시스템 적용

### 5. 🔐 보안 및 인증 (Security)
- **JWT 인증**: Access Token 기반 인증
- **이메일 인증**: 회원가입 및 비밀번호 찾기 시 이메일 코드를 통한 본인 확인

## ⚙️ 환경 변수 설정 (.env)

```env
# Server
APP_NAME=MINYOU
SERVER_PORT=8080

# Database
DB_URL=jdbc:mysql://localhost:3307/zip_chack?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password

# Kakao Map (REST API Key)
KAKAO_MAP_REST_API_KEY=your_kakao_rest_key

# Mail Server (Google SMTP)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

## 📄 라이선스
SSAFY 관통 프로젝트 - ZIP-Chack Team