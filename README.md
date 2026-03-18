# fortune_index_api

사주, 타로, 주식 시세를 결합해 투자 상담 결과를 생성하고 이력을 저장하는 Spring Boot API입니다.  
현재 구현은 `하이브리드 상담 API`, `상담 이력 조회/회고 API`, `KIS 시세 연동 API`, `내부 가상투자 기록 API`를 중심으로 구성되어 있습니다.

## 현재 구현 범위

### 1. 종합 상담
- `POST /api/consult`
- 주식 시세 조회
- 사주 분석
- 타로 3장 해석
- Gemini 기반 JSON 응답 생성
- 상담 이력 DB 저장
- 공유용 `shareKey` 발급

지원 모드:
- `ONLY_STOCK`
- `STOCK_SAJU`
- `STOCK_TAROT`
- `STOCK_ALL`

부가 조회:
- `GET /api/scenarios`
- `GET /api/history?userId={id}`
- `GET /api/history/{historyId}?userId={id}`
- `GET /api/history/share/{shareKey}`

### 2. 상담 이력/회고
- `GET /api/users/{userId}/consulting-histories`
- `GET /api/users/{userId}/consulting-histories/{historyId}`
- `PATCH /api/users/{userId}/consulting-histories/{historyId}/retro`
- `GET /api/users/{userId}/consulting-histories/retro/stats`

지원 내용:
- 사용자별 상담 이력 페이징 조회
- 상세 조회
- 회고 피드백, 실현 수익률, 메모 저장
- 회고 통계 집계

### 3. KIS 연동
- `GET /api/kis/market/sectors/index-price`
- `GET /api/kis/market/sectors/constituents`
- `GET /api/kis/market/holiday`
- `GET /api/kis/market/rankings/top-updown`

지원 내용:
- 업종 지수 조회
- 업종 구성 종목 조회
- 휴장일 조회
- 상승/하락 상위 종목 조회

### 4. 내부 가상투자 기록
- `POST /api/users/{userId}/virtual-investments`
- `GET /api/users/{userId}/virtual-investments`

지원 내용:
- 사용자가 버튼을 누른 시점의 실제 시장가를 매수 기준가로 저장
- 저장된 기준가 대비 현재 실제 시세를 반영해 수익률 계산
- 보유 중인 가상투자 포지션 조회

### 5. 내부 구현된 도메인
- 사주 간지 계산기와 상담용 분석 DTO
- 타로 카드 78장 정의 및 3장 리딩
- 가상 투자 포지션 수익률 계산 서비스
- LLM 프롬프트 템플릿 DB 시딩
- 공통 코드 SQL 초기화

## 아직 없는 것

현재 코드 기준으로 아래는 보이지 않습니다.
- 사용자 생성/수정/삭제 API
- 가상 투자 매수/조회용 외부 공개 Controller
- 인증/인가
- 운영용 배포 스크립트

즉, 상담과 KIS 연동 백엔드 코어는 구현되어 있지만, 사용자 관리와 운영 배포 영역은 아직 얇습니다.

## 기술 스택
- Kotlin 2.2
- Spring Boot 3.3
- Spring Web / Validation / Data JPA
- Spring Cloud OpenFeign
- H2, MySQL Connector
- springdoc OpenAPI
- Gemini API
- ICU4J

## 실행 환경
- Java 17
- 기본 프로파일: `h2`

기본 실행 시 H2 메모리 DB를 사용합니다.

개발용 MySQL 프로필은 `dev` 입니다.

## 환경 변수

### Logging
- `LOG_PATH`

### AI
- `GEMINI_API_KEY`

### KIS
- `KIS_APP_KEY`
- `KIS_APP_SECRET`

외부 키가 없으면 일부 기능은 실패하거나 fallback 동작을 합니다.
- 주식 시세 조회 실패 시 내부적으로 `fallback=true` 와 `0` 가격으로 내려갈 수 있습니다.
- AI 키가 없으면 `/api/consult` 는 정상 응답을 만들 수 없습니다.

로그 파일 경로를 지정하지 않으면 로컬/개발 실행에서는 `/tmp/fortune_index/logs` 를 사용합니다.
Docker 이미지는 `LOG_PATH=/data/logs` 로 고정되며, 런타임 사용자(`spring`)가 해당 디렉터리에 쓸 수 있도록 생성합니다.

## 실행 방법

```bash
./gradlew bootRun
```

`dev` 프로필로 MySQL에 붙으려면 아래 환경변수를 주고 실행합니다.

```bash
SPRING_PROFILES_ACTIVE=dev \
DB_USERNAME=fortune_index \
DB_PASSWORD=change-me \
JWT_SECRET=change-me-change-me-change-me-change-me \
./gradlew bootRun
```

## Docker

멀티스테이지 [`Dockerfile`](/Users/digitalmedic_hw/hwdev/fortune_index_api/Dockerfile) 이 추가되어 있습니다. 빌드 스테이지는 Gradle Alpine 이미지를 사용하고, 런타임 스테이지는 `eclipse-temurin:17-jre-alpine` 기반입니다.

이미지 빌드:

```bash
docker build -t fortune-index-api:local .
```

## GitHub Actions

`develop` 브랜치에 push 되면 [`docker-image-develop.yml`](/Users/digitalmedic_hw/hwdev/fortune_index_api/.github/workflows/docker-image-develop.yml) 이 Docker 이미지를 빌드해서 Docker Hub로 push 합니다.

권장 설정:
- 이미지 경로: `docker.io/<DOCKER_USERNAME>/fortune-index-api:develop`
- 추가 태그: 커밋 SHA, `latest`

GitHub에 등록할 시크릿:
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`
- `DEV_DB_USERNAME`
- `DEV_DB_PASSWORD`
- `DEV_DB_ROOT_PASSWORD`
- `JWT_SECRET`
- `MAIL_FROM_ADDRESS`
- `GEMINI_API_KEY`
- `KIS_APP_KEY`
- `KIS_APP_SECRET`

현재 워크플로는 이미지 빌드/푸시까지만 수행합니다. 앱 설정은 이미 환경변수 기반이라, 샘플처럼 `application.yml` 값을 워크플로에서 치환하지 않고 런타임 시크릿으로 유지하도록 구성했습니다.

애플리케이션 실행 후 확인 가능한 기본 경로:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console`

## 예시 요청

### 종합 상담 요청

```http
POST /api/consult
Content-Type: application/json

{
  "userId": 1,
  "mode": "STOCK_ALL",
  "scenario": "TIMING_ENTRY",
  "stockCode": "005930",
  "stockName": "삼성전자",
  "tarotIndices": [0, 10, 21],
  "tarotInterpretationMode": "MAIN_TRADITIONAL"
}
```

주의:
- `STOCK_TAROT`, `STOCK_ALL` 처럼 타로가 포함된 모드에서는 `tarotIndices` 가 필요합니다.
- 사용자 데이터가 미리 DB에 없으면 `404 user not found` 가 발생합니다.

## 개발 메모

- 기본 프로파일은 [`application.yml`](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/resources/application.yml) 에서 `h2` 로 설정되어 있습니다.
- H2 설정은 [`application-h2.yml`](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/resources/application-h2.yml) 에 있습니다.
- 상담 진입 API는 [`ConsultingService.kt`](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingService.kt) 에 있습니다.
- KIS 연동 API는 [`KisIntegrationController.kt`](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/market/KisIntegrationController.kt) 에 있습니다.
- 상담 이력 API는 [`ConsultingHistoryController.kt`](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/history/ConsultingHistoryController.kt) 에 있습니다.

## 테스트

다음 명령으로 테스트를 통과했습니다.

```bash
./gradlew test
```

현재 테스트는 사주 계산, 타로 서비스, 가상 투자 수익률 계산, 스프링 부트 기동 여부를 포함합니다.
