# Alert API

## 1. FCM 토큰 등록

- **URL**: `POST /api/fcm/register`

    - **Request Body**
      ```json
      [
        {
          "fcmtoken": "fcm_token_value"
        }
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

## 2. SSE 구독

- **URL**: `GET /api/alert/subscribe`
- **Headers**

  | 헤더명           | 설명                                                      |
    |---------------|---------------------------------------------------------|
  | Authorization | `Bearer <token>` Bearer 토큰                                     |
  | Last-Event-ID | `20240604_abcdef123456` 클라이언트의 마지막 수신 이벤트 ID (재연결 시 사용) |
- **Response (200)**
  ```text
  event: SSE_ALERT_INIT
  id: 20240604_abcdef123456
  data: connected
  ```
---

## 3. 전체 알림 목록 조회

- **URL**: `GET  /api/alert`
- **Headers**

  | 헤더명           | 설명               |
  |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Response (200)**

  ```json
  {
   "isSuccess": true,
   "code": 1000,
   "message": "요청에 성공하였습니다.",
   "result": [
    {
      "alertId": 1,
      "alertStatusId": 3,
      "id": "686692198126160f",
      "title": "[ERROR] ssok-bank",
      "message": "Authentication error: Authorization header is missing or invalid",
      "kind": "OPENSEARCH",
      "isRead": false,
      "timestamp": "2025-05-30T07:24:06.396205638+00:00",
      "createdAt": "2025-05-30T07:24:06.396205638+00:00",
      "employeeId": "123456"
    },
    {
      "alertId": 2,
      "alertStatusId": 4,
      "id": "9fbb1234567890",
      "title": "[INFO] jenkins-ssok-bank",
      "message": "Build completed successfully",
      "kind": "JENKINS",
      "isRead": true,
      "timestamp": "2025-05-29T10:15:00.000000000+00:00",
      "createdAt": "2025-05-29T10:15:05.000000000+00:00",
      "employeeId": "123456"
    }
    ]
  }
  ```

---

## 4. 페이징 알림 목록 조회

- **URL**: `GET  /api/alert/paged`
- **Headers**

  | 헤더명           | 설명               |
  |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Request Params**

  | 매개변수  | 타입     | 필수 여부 | 설명                                    |
  |-------|--------|-------|---------------------------------------|
  | page   | int    | false | 조회할 페이지 번호 (0부터 시작) |
  | size | String | false | 한 페이지에 표시할 알림 개수                |
  | sort | String | false | 정렬 기준 (ex. alert.timestamp,DESC)                |

- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 1000,
    "message": "요청에 성공하였습니다.",
    "result": {
    "content": [
      {
        "alertId": 1,
        "alertStatusId": 3,
        "id": "686692198126160f",
        "title": "[ERROR] ssok-bank",
        "message": "Authentication error: Authorization header is missing or invalid",
        "kind": "OPENSEARCH",
        "isRead": false,
        "timestamp": "2025-05-30T07:24:06.396205638+00:00",
        "createdAt": "2025-05-30T07:24:06.396205638+00:00",
        "employeeId": "123456"
      },
      {
        "alertId": 2,
        "alertStatusId": 4,
        "id": "9fbb1234567890",
        "title": "[INFO] jenkins-ssok-bank",
        "message": "Build completed successfully",
        "kind": "JENKINS",
        "isRead": true,
        "timestamp": "2025-05-29T10:15:00.000000000+00:00",
        "createdAt": "2025-05-29T10:15:05.000000000+00:00",
        "employeeId": "123456"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
     },
     "totalPages": 5,
     "totalElements": 50,
     "last": false,
     "first": true,
     "numberOfElements": 10,
     "empty": false
     }
    }
  ```

---

## 5. 알림 개별 상태 변경

- **URL**: `PATCH  /api/alert/modify`
- **Headers**

  | 헤더명           | 설명               |
  |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Request Body**
  ```json
  [
    {
      "alertStatusId": 1234,
      "isRead": true
    }
  ]
  ```
- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 1000,
    "message": "요청에 성공하였습니다.",
    "result": {
      "alertId": 1,
      "alertStatusId": 2,
      "id": "686692198126160f",
      "title": "[ERROR] ssok-bank",
      "message": "Authentication error: Authorization header is missing or invalid",
      "kind": "OPENSEARCH",
      "isRead": true,
      "timestamp": "2025-05-30T07:24:06.396205638+00:00",
      "createdAt": "2025-05-30T07:24:06.396205638+00:00",
      "employeeId": "emp123456"
     }
    }
  ```

---

## 6. 알림 일괄 상태 변경

- **URL**: `PATCH  /api/alert/modifyAll`
- **Headers**

  | 헤더명           | 설명               |
  |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Response (200)**

  ```json
    {
    "isSuccess": true,
    "code": 1000,
    "message": "요청에 성공하였습니다.",
    "result": [
    {
      "alertId": 1,
      "alertStatusId": 2,
      "id": "686692198126160f",
      "title": "[ERROR] ssok-bank",
      "message": "Authentication error: Authorization header is missing or invalid",
      "kind": "OPENSEARCH",
      "isRead": true,
      "timestamp": "2025-05-30T07:24:06.396205638+00:00",
      "createdAt": "2025-05-30T07:24:06.396205638+00:00",
      "employeeId": "emp123456"
    },
    {
      "alertId": 2,
      "alertStatusId": 3,
      "id": "4f9c72da6aeb6db2",
      "title": "[INFO] ssok-system",
      "message": "정상적으로 작동 중입니다.",
      "kind": "GRAFANA",
      "isRead": true,
      "timestamp": "2025-05-30T08:00:00.000000000+00:00",
      "createdAt": "2025-05-30T08:00:00.000000000+00:00",
      "employeeId": "emp123456"
    }
   ]
  }
  ```

---

## 7. 알림 개별 삭제

- **URL**: `PATCH /api/alert/delete`
- **Headers**

  | 헤더명           | 설명               |
  |---------------|------------------|
  | Authorization | `Bearer <token>` |

- **Request Body**
  ```json
    {
     "alertStatusId": 1234
    }
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

## 8. Grafana 알림

- **URL**: `POST  /api/alert/grafana`
- **Request Body**
  ```json
  {
    "alerts": [
      {
        "app": "ssok-bluetooth-service",
        "level": "warning",
        "message": "KubePodCrashLooping - Pod ssok/ssok-bluetooth-service-558488498d-7v4pg (ssok-bluetooth-service) is in waiting state (reason: \"CrashLoopBackOff\") on cluster .",
        "timestamp": "2025-05-31T00:14:01.472Z",
        "id": "686692198126160f"
      },
      {
        "app": "ssok-bluetooth-service",
        "level": "warning",
        "message": "KubePodCrashLooping - Pod ssok/ssok-bluetooth-service-558488498d-7v4pg (ssok-bluetooth-service) is in waiting state (reason: \"CrashLoopBackOff\") on cluster .",
        "timestamp": "2025-05-31T00:14:01.472Z",
        "id": "686692198126160f"
      }
    ]
  }
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

## 9. Opensearch Dashboard 알림

- **URL**: `POST  /api/alert/opensearch`
  - **Request Body**
    ```json
    {
      {
      "id": "wh8TIJcBfhJZWUSwqRZX",
      "level": "ERROR",
      "app": "ssok-gateway-service",
      "timestamp": "2025-05-30T07:24:06.396205638+00:00",
      "message": "Authentication error: Authorization header is missing or invalid"
    },
    {
      "id": "wh8TIJcBfhJZWUSwqRZX",
      "level": "ERROR",
      "app": "ssok-gateway-service",
      "timestamp": "2025-05-30T07:24:06.396205638+00:00",
      "message": "Authentication error: Authorization header is missing or invalid"
    },
    }
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

## 10. Github 이슈 알림

- **URL**: `POST  /api/alert/issue`
- **Request Body**
  ```json
  {
    "action": "opened",
	   "issue": {
		  "assignees": [
				{
				"login": "octocat"
				}
			],
	  "created_at": "2025-06-07T12:34:56Z"
	 }
  }
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

## 11. Devops 알림

- **URL**: `POST  /api/alert/devops`
- **Request Body**
  ```json
  {
    "level": "INFO",
    "app": "jenkins_ssok-bank",
    "timestamp": "2025-06-04T12:00:00Z",
    "message": "빌드가 성공적으로 완료되었습니다."
  }
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