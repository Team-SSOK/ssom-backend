# Logging API

## 1. 오픈서치 실시간 로그 알림

- **URL**: `POST /api/logging/opensearch`

    - **Request Body**
      ```json
      [
        {
          "logId": "Pz_4OZcBEKdnQBbepWWz",
          "timestamp": "2025-06-04T08:04:46.033104672+00:00",
          "level": "ERROR",
          "logger": "kr.ssok.gateway.security.filter.JwtAuthenticationFilter",
          "thread": "reactor-http-epoll-1",
          "message": "Authentication error: Authorization header is missing or invalid",
          "app": "ssok-gateway-service"
        },
        {
          "logId": "vT8qOZcBEKdnQBbeZ1T0",
          "timestamp": "2025-06-04T04:18:30.664282298+00:00",
          "level": "WARN",
          "logger": "kr.ssok.accountservice.service.impl.AccountServiceImpl",
          "thread": "http-nio-8080-exec-4",
          "message": "[GET] Account not found: userId=51",
          "app": "ssok-account-service"
        },
      ]
      ```

    - **Response (200)**
      ```json
        {
          "isSuccess": true,
          "code": 1000,
          "message": "요청에 성공하였습니다.",
          "result": null
        }
      ```

---

## 2. 실시간 로그 SSE 구독

- **URL**: `GET  /api/logging/subscribe`
- **Headers**

  | 헤더명           | 설명               |
    |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Request Params**

  | 매개변수 | 타입 | 필수 여부 | 설명 |
      | --- | --- | --- | --- |
  | app | String | false | 사용자의 현재 필터링 조건 |
  | level | String | false | 사용자의 현재 필터링 조건 |

---

## 3. 서비스 목록 조회

- **URL**: `GET  /api/logging/services`
- **Headers**

  | 헤더명           | 설명               |
      |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 2000,
    "message": "요청에 성공하였습니다.",
    "result": {
        "services": [
            {
                "serviceName": "ssok-gateway-service",
                "count": 1334
            },
            {
                "serviceName": "ssok-user-service",
                "count": 6
            },
            {
                "serviceName": "ssok-account-service",
                "count": 2
            }
        ]
    }
    }
  ```

---

## 4. 로그 목록 조회

- **URL**: `GET  /api/logging`
- **Headers**

  | 헤더명           | 설명               |
        |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Request Params**

  | 매개변수 | 타입 | 필수 여부 | 설명           |
        | --- | --- | --- |--------------|
  | app | String | false | 로그가 발생한 서비스 (예: ssok-account-service) |
  | level | String | false | 로그 레벨 (예: WARN, ERROR)       |

- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 2000,
    "message": "요청에 성공하였습니다.",
    "result": {
        "logs": [
            {
                "logId": "Pz_4OZcBEKdnQBbepWWz",
                "timestamp": "2025-06-04T08:04:46.033104672+00:00",
                "level": "ERROR",
                "logger": "kr.ssok.gateway.security.filter.JwtAuthenticationFilter",
                "thread": "reactor-http-epoll-1",
                "message": "Authentication error: Authorization header is missing or invalid",
                "app": "ssok-gateway-service"
            },
            {
                "logId": "vT8qOZcBEKdnQBbeZ1T0",
                "timestamp": "2025-06-04T04:18:30.664282298+00:00",
                "level": "WARN",
                "logger": "kr.ssok.accountservice.service.impl.AccountServiceImpl",
                "thread": "http-nio-8080-exec-4",
                "message": "[GET] Account not found: userId=51",
                "app": "ssok-account-service"
            },
            {
                "logId": "sz8qOZcBEKdnQBbeUlTj",
                "timestamp": "2025-06-04T04:18:20.086907305+00:00",
                "level": "ERROR",
                "logger": "kr.ssok.gateway.security.filter.JwtAuthenticationFilter",
                "thread": "reactor-http-epoll-2",
                "message": "Authentication error: Authorization header is missing or invalid",
                "app": "ssok-gateway-service"
            }
        ]
    }
    }
  ```

---

## 5. 로그 목록 조회 (무한 스크롤 방식)

- **URL**: `GET  /api/logging/infinitescroll`
- **Headers**

  | 헤더명           | 설명               |
          |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Request Params**

  | 매개변수 | 타입 | 필수 여부 | 설명 |
    | --- | --- | --- | --- |
  | app | String | false | 로그가 발생한 서비스 (예: ssok-account-service) |
  | level | String | false | 로그 레벨 (예: WARN, ERROR) |
  | searchAfterTimestamp | String | false | 이전에 마지막으로 조회한 로그의 timestamp |
  | searchAfterId | String | false | 이전에 마지막으로 조회한 로그의 로그 ID |

- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 2000,
    "message": "요청에 성공하였습니다.",
    "result": {
        "logs": [
            {
                "logId": "lj8nP5cBEKdnQBbeGJZP",
                "timestamp": "2025-06-05T08:13:28.652118686+00:00",
                "level": "ERROR",
                "logger": "kr.ssok.gateway.security.filter.JwtAuthenticationFilter",
                "thread": "reactor-http-epoll-1",
                "message": "Authentication error: Authorization header is missing or invalid",
                "app": "ssok-gateway-service"
            },
            {
                "logId": "LT8fP5cBEKdnQBbe45bK",
                "timestamp": "2025-06-05T08:04:42.078518420+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-6-thread-1",
                "message": "sourceName : ssok-notification-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-notification-service"
            },
            {
                "logId": "Pz8fP5cBEKdnQBbe5ZYs",
                "timestamp": "2025-06-05T08:04:41.774698618+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-4-thread-1",
                "message": "sourceName : ssok-account-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-account-service"
            },
            {
                "logId": "KT8fP5cBEKdnQBbe45bK",
                "timestamp": "2025-06-05T08:04:41.765303920+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-6-thread-1",
                "message": "sourceName : ssok-notification-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-notification-service"
            },
            {
                "logId": "Pj8fP5cBEKdnQBbe5ZYm",
                "timestamp": "2025-06-05T08:04:41.747062879+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-4-thread-1",
                "message": "sourceName : ssok-user-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-user-service"
            },
            {
                "logId": "PD8fP5cBEKdnQBbe5ZYO",
                "timestamp": "2025-06-05T08:04:41.724444657+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-3-thread-1",
                "message": "sourceName : ssok-transfer-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-transfer-service"
            },
            {
                "logId": "PT8fP5cBEKdnQBbe5ZYS",
                "timestamp": "2025-06-05T08:04:41.721298471+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-4-thread-1",
                "message": "sourceName : ssok-bluetooth-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-bluetooth-service"
            },
            {
                "logId": "MT8fP5cBEKdnQBbe5JY6",
                "timestamp": "2025-06-05T08:04:41.711991336+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-6-thread-1",
                "message": "sourceName : ssok-gateway-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-gateway-service"
            },
            {
                "logId": "ID8eP5cBEKdnQBbe0par",
                "timestamp": "2025-06-05T08:04:28.530260425+00:00",
                "level": "ERROR",
                "logger": "kr.ssok.gateway.security.filter.JwtAuthenticationFilter",
                "thread": "reactor-http-epoll-1",
                "message": "Authentication error: Authorization header is missing or invalid",
                "app": "ssok-gateway-service"
            },
            {
                "logId": "_D8dP5cBEKdnQBbe0JW7",
                "timestamp": "2025-06-05T08:02:26.252756347+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-6-thread-1",
                "message": "sourceName : ssok-gateway-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-gateway-service"
            },
            {
                "logId": "5z8dP5cBEKdnQBbeUJW0",
                "timestamp": "2025-06-05T08:02:25.909397610+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-3-thread-1",
                "message": "sourceName : ssok-transfer-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-transfer-service"
            },
            {
                "logId": "9D8dP5cBEKdnQBbeUJXN",
                "timestamp": "2025-06-05T08:02:25.893761620+00:00",
                "level": "WARN",
                "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
                "thread": "pool-4-thread-1",
                "message": "sourceName : ssok-user-service-kubernetes was requested, but not found in namespace : ssok",
                "app": "ssok-user-service"
            }
        ],
        "lastTimestamp": "1749110545893",
        "lastLogId": "9D8dP5cBEKdnQBbeUJXN"
    }
    }
  ```

---

## 6. 특정 로그에 대한 LLM 분석 조회

- **URL**: `GET  /api/logging/analysis/{logId}`
- **Headers**

  | 헤더명           | 설명               |
        |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 2000,
    "message": "요청에 성공하였습니다.",
    "result": {
        "summary": "userId에 해당하는 계좌가 존재하지 않아 AccountNotFound 예외 발생",
        "location": {
            "file": "AccountInternalServiceImpl.java",
            "function": "findAllAccountIds()"
        },
        "solution": "사용자 ID로 조회 시 계좌가 없으면 적절한 예외 처리와 함께 계좌 등록 여부를 확인하는 로직 추가 필요",
        "solution_detail": "1. AccountInternalServiceImpl.java 파일에서 findAllAccountIds(Long userId) 메서드 확인\n2. accountRepository.findByUserIdAndIsDeletedFalse(userId) 호출 결과가 빈 리스트일 경우 AccountException(AccountResponseStatus.ACCOUNT_NOT_FOUND) 예외를 발생시키는 현재 로직 유지\n3. 사용자에게 계좌가 없다는 명확한 메시지를 전달하도록 AccountResponseStatus.ACCOUNT_NOT_FOUND 상태 메시지 검토 및 필요 시 개선\n4. 사용자 계좌가 없을 경우를 대비해 프론트엔드 또는 호출 서비스에서 계좌 등록 유도 UI/로직 추가 권장\n5. 배포 전 단위 테스트 및 통합 테스트에서 사용자 계좌가 없을 때 예외가 정상 발생하는지 검증\n6. 운영 환경에서 동일 오류 발생 시 사용자 데이터 및 DB 상태 점검하여 계좌 데이터 누락 여부 확인\n7. 필요 시 사용자 계좌 데이터 생성 프로세스 점검 및 보완\n\n- 테스트 커맨드 예시:\n  ./gradlew test --tests \"kr.ssok.accountservice.service.impl.AccountInternalServiceImplTest.findAllAccountIds_Empty\"\n\n- 주의사항:\n  - 예외 메시지 및 상태 코드를 명확히 하여 호출 서비스가 적절히 대응할 수 있도록 할 것\n  - 사용자 ID가 올바른지, 계좌 데이터가 정상적으로 저장되고 있는지 DB 상태를 점검할 것"
    }
    }
  ```

---

## 7. 특정 로그에 대한 LLM 분석 생성

- **URL**: `POST /api/logging/analysis`
- **Headers**

  | 헤더명           | 설명               |
        |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Request Body**
  ```json
    {
    "logId": "vD8qOZcBEKdnQBbeZ1T0",
    "timestamp": "2025-06-04T04:18:28.196068611+00:00",
    "level": "WARN",
    "logger": "kr.ssok.accountservice.service.impl.AccountServiceImpl",
    "thread": "http-nio-8080-exec-3",
    "message": "[GET] Account not found: userId=51",
    "app": "ssok-account-service"
    }
  ```

- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 2000,
    "message": "요청에 성공하였습니다.",
    "result": {
        "summary": "userId에 해당하는 계좌가 존재하지 않아 AccountNotFound 예외 발생",
        "location": {
            "file": "AccountInternalServiceImpl.java",
            "function": "findAllAccountIds()"
        },
        "solution": "사용자 ID로 조회 시 계좌가 없으면 적절한 예외 처리와 함께 계좌 등록 여부를 확인하는 로직 추가 필요",
        "solution_detail": "1. AccountInternalServiceImpl.java 파일에서 findAllAccountIds(Long userId) 메서드 확인\n2. accountRepository.findByUserIdAndIsDeletedFalse(userId) 호출 결과가 빈 리스트일 경우 AccountException(AccountResponseStatus.ACCOUNT_NOT_FOUND) 예외를 발생시키는 현재 로직 유지\n3. 사용자에게 계좌가 없다는 명확한 메시지를 전달하도록 AccountResponseStatus.ACCOUNT_NOT_FOUND 상태 메시지 검토 및 필요 시 개선\n4. 사용자 계좌가 없을 경우를 대비해 프론트엔드 또는 호출 서비스에서 계좌 등록 유도 UI/로직 추가 권장\n5. 배포 전 단위 테스트 및 통합 테스트에서 사용자 계좌가 없을 때 예외가 정상 발생하는지 검증\n6. 운영 환경에서 동일 오류 발생 시 사용자 데이터 및 DB 상태 점검하여 계좌 데이터 누락 여부 확인\n7. 필요 시 사용자 계좌 데이터 생성 프로세스 점검 및 보완\n\n- 테스트 커맨드 예시:\n  ./gradlew test --tests \"kr.ssok.accountservice.service.impl.AccountInternalServiceImplTest.findAllAccountIds_Empty\"\n\n- 주의사항:\n  - 예외 메시지 및 상태 코드를 명확히 하여 호출 서비스가 적절히 대응할 수 있도록 할 것\n  - 사용자 ID가 올바른지, 계좌 데이터가 정상적으로 저장되고 있는지 DB 상태를 점검할 것"
    }
    }
  ```

---

## 8. 로그 상세 조회

- **URL**: `GET  /api/logging/{logId}`
- **Headers**

  | 헤더명           | 설명               |
        |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 2000,
    "message": "요청에 성공하였습니다.",
    "result": {
        "logId": "Pj8fP5cBEKdnQBbe5ZYm",
        "timestamp": "2025-06-05T08:04:41.747062879+00:00",
        "level": "WARN",
        "logger": "org.springframework.cloud.kubernetes.commons.config.ConfigUtils",
        "thread": "pool-4-thread-1",
        "message": "sourceName : ssok-user-service-kubernetes was requested, but not found in namespace : ssok",
        "app": "ssok-user-service"
    }
    }
  ```

---