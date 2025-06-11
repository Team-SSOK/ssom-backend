# Logging Module

SSOM 앱의 로그 관련 기능를 담당하는 모듈입니다.

## 주요 기능

- 서비스 목록 조회
- 로그 목록 조회
- 특정 로그 상세 조회
- 특정 로그에 대한 LLM 로그 분석 요청
- 로그 SSE

## 구조

```
logging/
├── controller/              # api
│   ├── LoggingController.java        # 로그 기능 관련 api
├── dto/                     # 데이터 전달 객체
│   ├── LogDataDto.java               # OpenSearch로부터 로그 정보를 받을 때 사용하는 dto
│   ├── LogDto.java                   # 로그 정보를 전달하는 dto
│   ├── LogResponseDto.java           # 로그 정보 리스트를 전달하는 dto
│   ├── LogScrollResponseDto.java     # 로그 정보 리스트를 전달하는 dto (무한 스크롤 방식으로 로그 목록 조회 시)
│   ├── ServiceDto.java               # 서비스 정보를 전달하는 dto
│   └── ServiceResponseDto.java       # 서비스 정보 리스트를 전달하는 dto
├── entity/                  # 실제 DB와 매핑되는 객체
│   └── LogSummary.java               # LLM을 이용한 로그 분석 정보를 저장
├── repository/              # DB에 접근하기 위한 인터페이스
│   └── LogSummaryRepository.java     # 로그 분석 정보를 저장한 DB에 접근
├── service/                 # application
│   ├── Impl/                    # 구현체
│   │   └── LoggingServiceImpl.java   # 로그 기능 관련 서비스 구현체
│   └── LoggingService.java           # 로그 기능 관련 서비스 인터페이스
├── sse/                     # SSE 기능을 위한 보조 클래스 모음
│   └── EmitterWithFilter.java        # 사용자가 현재 적용 중인 필터링 조건을 SSE Emitter와 묶어주는 객체
├── transfer-api-spec.md     # API 명세서
└── README.md                # 모듈 문서
```

## API 명세

자세한 API 명세는 [logging_api_spec.md](./logging_api_spec.md)를 참조하세요.

### 주요 API 엔드포인트

- `POST /api/logging/opensearch`: 오픈서치 실시간 로그 알림
- `GET  /api/logging/subscribe`: 실시간 로그 SSE 구독
- `GET  /api/logging/services`: 서비스 목록 조회
- `GET  /api/logging`: 로그 목록 조회
- `GET  /api/logging/infinitescroll`: 로그 목록 조회 (무한 스크롤 방식)
- `GET  /api/logging/analysis/{logId}`: 특정 로그에 대한 LLM 분석 조회
- `POST /api/logging/analysis`: 특정 로그에 대한 LLM 분석 생성
- `GET  /api/logging/{logId}`: 로그 상세 조회

## 에러 처리

### 주요 에러 메시지
```java
public enum BaseResponseStatus {
    // SSE 구독 시도 관련
    UNAUTHORIZED(false, 4003, "인증되지 않은 사용자입니다."),

    // SSE 처리 관련
    SSE_INIT_ERROR(false, 7002, "SSE 구독 처리 중 오류가 발생했습니다."),
    PARSING_ERROR(false, 7004, "Json Parsing 오류가 발생했습니다."),

    // Logging 관련 오류
    SERVICES_READ_FAILED(false, 8001, "OpenSearch에서 서비스 목록 조회에 실패했습니다."),
    LOGS_READ_FAILED(false, 8002, "OpenSearch에서 로그 목록 조회에 실패했습니다."),
    LOG_SUMMARY_NOT_FOUND(false, 8003, "기존에 생성된 LLM 요약이 없습니다."),
    LLM_SUMMARY_FAILED(false, 8004, "LLM 서비스를 이용한 로그 분석 생성에 실패했습니다."),
    LLM_SUMMARY_SAVE_FAILED(false, 8005, "LLM 로그 분석을 저장하는 데 실패했습니다."),
    LOG_NOT_FOUND(false, 8006, "로그 ID로 로그를 조회하는 데 실패했습니다."),

    // 서버 오류
    INTERNAL_SERVER_ERROR(false, 5000, "서버 내부 오류가 발생했습니다.");
}
```

## 의존성

### 외부 라이브러리
- `lombok`
- `jakarta`
- `swagger`
- `jackson`
- `opensearch`

### 내부 의존성
- `@/global`: 공통 모듈
- `@/domain/user`: 유저 모듈
