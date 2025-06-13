# Alert Module

SSOM 앱의 알림 관련 기능를 담당하는 모듈입니다.

## 주요 기능

- FCM 토큰 등록
- SSE 알림 구독
- 전체 알림 목록 조회
- 페이징 알림 목록 조회
- 알림 개별 상태 변경
- 알림 일괄 상태 변경
- 알림 개별 삭제
- Grafana 알림
- Opensearch Dashboard 알림
- Github 이슈 알림
- Jenkins 및 ArgoCD 알림

## 구조

```
alert/
├── controller/              # api
│   ├── fcm/                     # fcm 관련 모듈
│   │   └── FcmController.java      # fcm 토큰 등록 api
│   ├── AlertController.java     # 알림 기능 관련 api
├── dto/                     # 데이터 전달 객체
│   ├── fcm/                             # fcm 관련 모듈
│   │   └── FcmRegisterRequestDto.java             # FCM 토큰 등록 요청 DTO
│   ├── AlertDevopsRequestDto.java       # DevOps에서 보내주는 포맷 dto
│   ├── AlertGrafanaRequestDto.java      # Grafana에서 보내주는 포맷 dto
│   ├── AlertIssueRequestDto.java        # Github에서 이슈 작업 시 보내주는 포맷 dto
│   ├── AlertModifyRequestDto.java       # 알림의 읽음 상태 변경을 위해 보내주는 dto
│   ├── AlertOpensearchRequestDto.java   # Opensearch에서 보내주는 포맷 dto
│   ├── AlertRequestDto.java             # 알림 저장을 위한 공통 포맷 dto
│   └── AlertResponseDto.java            # 알림 전송을 위한 공통 포맷 dto
├── entity/                  # 실제 DB와 매핑되는 객체
│   ├── constant/                     # enum 관련 모듈
│   │   └── AlertKind.java               # 알림 종류를 정의한 enum
│   ├── Alert.java                    # 알림으로 전송할 정보를 저장
│   └── AlertStatus.java              # 알림의 상태 관리를 위한 정보를 저장
├── exception/               # DB에 접근하기 위한 인터페이스
│   └── SseExceptionHandler.java     # SSE 관련 예외를 처리하는 전용 Exception Handler
├── repository/              # DB에 접근하기 위한 인터페이스
│   ├── AlertRepository.java         # 알림 정보를 저장한 DB에 접근
│   └── AlertStatusRepository.java   # 알림 상태 정보를 저장한 DB에 접근
├── scheduler/               # SSE 기능을 위한 보조 클래스 모음
│   └── SseConnectionMonitor.java    # SSE 연결 상태를 모니터링하고 정리하는 스케줄러
├── service/                 # application
│   ├── fcm/                    # fcm 관련 모듈
│   │   ├── FcmService.java          # FCM 기능 관련 서비스 인터페이스
│   │   └── FcmServiceImpl.java      # FCM 기능 관련 서비스 구현체
│   ├── AlertService.java       # 알림 기능 관련 서비스 인터페이스
│   └── AlertServiceImpl.java   # 알림 기능 관련 서비스 구현체
├── alert_api_spec.md      # API 명세서
└── README.md                # 모듈 문서
```

## API 명세

자세한 API 명세는 [alert_api_spec.md](./alert_api_spec.md)를 참조하세요.

### 주요 API 엔드포인트

- `GET    /api/alert/subscribe`: SSE 구독
- `GET    /api/alert`: 전체 알림 목록 조회
- `GET    /api/alert/paged`: 페이징 알림 목록 조회
- `PATCH  /api/alert/modify`: 알림 개별 상태 변경
- `PATCH  /api/alert/modifyAll`: 알림 일괄 상태 변경
- `PATCH  /api/alert/delete`: 알림 개별 삭제
- `POST   /api/alert/register`: FCM 토큰 등록
- `POST   /api/alert/grafana`: Grafana 알림
- `POST   /api/alert/opensearch`: Opensearch Dashboard 알림
- `POST   /api/alert/issue`: Github 이슈 알림
- `POST   /api/alert/devops`: Jenkins 및 ArgoCD 알림

## 에러 처리

### 주요 에러 메시지
```java
public enum BaseResponseStatus {
    // SSE 관련
    SSE_BAD_REQUEST(false, 7001, "SSE 구독을 위한 사용자 정보가 전달되지 않았습니다."),
    SSE_INIT_ERROR(false, 7002, "SSE 구독 처리 중 오류가 발생했습니다."),
    
    // 알림 관련 오류
    SSE_BAD_REQUEST(false, 7001, "SSE 구독을 위한 사용자 정보가 전달되지 않았습니다."),
    SSE_INIT_ERROR(false, 7002, "SSE 구독 처리 중 오류가 발생했습니다."),
    NOT_FOUND_ALERT(false, 7003, "Alert를 찾을 수 없습니다."),
    PARSING_ERROR(false, 7004, "Json Parsing 오류가 발생했습니다."),
    ALERT_TARGET_USER_NOT_FOUND(false, 7005, "Github 이슈 알림 공유 대상자가 조회되지 않습니다."),
    ALERT_CREATE_FAILED(false, 7006, "알림 생성 중 오류가 발생했습니다."),
    UNSUPPORTED_ALERT_KIND(false, 7007, "알림 유형이 유효하지 않습니다."),
    REDIS_ACCESS_FAILED(false, 7008, "Redis 접근에 실패하였습니다."),
    INVALID_REQUEST(false, 4002, "유효하지 않은 요청입니다."),

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
- `RedisTemplate`
- `SseEmitter`

### 내부 의존성
- `@/global`: 공통 모듈
- `@/domain/user`: 유저 모듈
