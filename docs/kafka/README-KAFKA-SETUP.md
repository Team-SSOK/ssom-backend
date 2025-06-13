# 🚀 SSOM Alert Kafka 비동기 처리 설정 가이드

## 📋 목차
- [개요](#-개요)
- [아키텍처](#-아키텍처)
- [환경 설정](#-환경-설정)
- [Kafka 클러스터 구성](#-kafka-클러스터-구성)
- [토픽 설계](#-토픽-설계)
- [성능 최적화](#-성능-최적화)
- [모니터링](#-모니터링)
- [운영 가이드](#-운영-가이드)
- [트러블슈팅](#-트러블슈팅)

---

## 🎯 개요

SSOM Backend의 Alert 시스템을 **동기 처리에서 Kafka 기반 비동기 처리**로 개선하여 성능을 획기적으로 향상시킨 프로젝트입니다.

### 🚀 성능 개선 효과
- **응답시간**: 3,468ms → 378ms (**89% 개선**)
- **처리량**: 0.29건/초 → 2.64건/초 (**약 9배 향상**)
- **확장성**: Consumer 인스턴스 증가로 처리량 선형 확장 가능
- **안정성**: 장애 시 재처리 보장, 메시지 순서 보장

---

## 🏗️ 아키텍처

### 기존 동기 처리 방식
```
[Client] → [AlertController] → [AlertService] 
    ↓
[대상 사용자 조회] → [AlertStatus 생성] → [SSE/FCM 전송]
    ↓ (순차 처리)
[응답 반환] (모든 처리 완료 후)
```

### 새로운 비동기 처리 방식
```
[Client] → [AlertController] → [AlertService] → [Alert 저장] → [즉시 응답]
    ↓
[Kafka Producer] → [alert-created-topic]
    ↓
[AlertKafkaConsumer] → [대상 사용자별 이벤트 발행]
    ↓
[user-alert-topic] → [병렬 Consumer] → [AlertStatus 생성 + SSE/FCM 전송]
```

### 🔄 이벤트 플로우
1. **Alert 생성**: API 요청으로 Alert 엔티티 저장
2. **이벤트 발행**: `AlertCreatedEvent`를 `alert-created-topic`에 발행
3. **대상 사용자 분석**: Consumer가 이벤트를 받아 대상 사용자 필터링
4. **개별 알림 이벤트**: 각 사용자별로 `UserAlertEvent`를 `user-alert-topic`에 발행
5. **병렬 처리**: 여러 Consumer가 병렬로 AlertStatus 생성 및 알림 전송

---

## ⚙️ 환경 설정

### 1. application.yml 설정

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    
    # Producer 설정
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: "1"                    # 리더만 확인
      retries: 3                   # 재시도 횟수
      batch-size: 16384           # 배치 크기 (16KB)
      linger-ms: 5                # 배치 대기 시간
      compression-type: lz4       # 압축 타입
      
    # Consumer 설정
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP:ssom-alert-consumer}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      auto-offset-reset: earliest  # 처음부터 읽기
      enable-auto-commit: false   # 수동 커밋
      max-poll-records: 10        # 한번에 처리할 레코드 수
      
    # 리스너 설정
    listener:
      concurrency: 10             # 동시 처리 스레드 수
      ack-mode: manual_immediate  # 수동 즉시 커밋

# Alert Kafka 전용 설정
alert:
  kafka:
    topics:
      alert-created: alert-created-topic
      user-alert: user-alert-topic
    processing:
      max-retry-attempts: 3
      timeout-ms: 30000
    monitoring:
      enable-metrics: true
      log-processing-time: true
```

### 2. 환경별 설정

#### 개발 환경 (dev)
```yaml
spring:
  config:
    activate:
      on-profile: dev
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ssom-alert-consumer-dev

alert:
  kafka:
    processing:
      max-retry-attempts: 1      # 빠른 실패
      batch-size: 10
```

#### 운영 환경 (prod)
```yaml
spring:
  config:
    activate:
      on-profile: prod
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka-cluster:9092}
    producer:
      acks: "all"                # 모든 복제본 확인
      retries: 10

alert:
  kafka:
    processing:
      max-retry-attempts: 5
      batch-size: 200
      timeout-ms: 60000
```

---

## 🐳 Kafka 클러스터 구성

### Docker Compose로 로컬 환경 구성

```bash
# Kafka 클러스터 시작
bash scripts/kafka/start-kafka.sh

# 클린 시작 (기존 데이터 삭제)
bash scripts/kafka/start-kafka.sh --clean
```

### 서비스 구성
- **Zookeeper**: 2181 포트
- **Kafka Broker**: 9092 포트
- **Kafdrop UI**: 9000 포트 (http://localhost:9000)
- **Schema Registry**: 8081 포트

### 토픽 생성
```bash
# 자동 토픽 생성
bash scripts/kafka/create-topics.sh

# 수동 토픽 생성
kafka-topics.sh --create \
  --topic alert-created-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

---

## 📊 토픽 설계

### 1. alert-created-topic
- **용도**: Alert 생성 이벤트 처리
- **파티션**: 3개 (낮은 처리량, 순서 중요)
- **메시지 구조**:
```json
{
  "alertId": 123,
  "alertKind": "DEPLOYMENT",
  "appName": "ssok-bank-api"
}
```

### 2. user-alert-topic
- **용도**: 사용자별 개별 알림 처리
- **파티션**: 10개 (높은 처리량, 병렬 처리)
- **파티셔닝 키**: userId (사용자별 순서 보장)
- **메시지 구조**:
```json
{
  "alertId": 123,
  "userId": "user001"
}
```

### 파티셔닝 전략
- **alert-created-topic**: alertId 기반 라운드 로빈
- **user-alert-topic**: userId 기반 (동일 사용자의 알림 순서 보장)

---

## ⚡ 성능 최적화

### 1. Producer 최적화
```yaml
# 배치 처리로 처리량 향상
batch-size: 16384              # 16KB 배치
linger-ms: 5                   # 5ms 대기 후 전송
buffer-memory: 33554432        # 32MB 버퍼

# 압축으로 네트워크 효율성
compression-type: lz4          # 빠른 압축/해제

# 신뢰성과 성능 균형
acks: "1"                      # 리더만 확인 (빠름)
retries: 3                     # 적절한 재시도
```

### 2. Consumer 최적화
```yaml
# 병렬 처리로 처리량 향상
concurrency: 10                # 10개 스레드 동시 처리
max-poll-records: 10          # 적절한 배치 크기

# 안정적인 처리
enable-auto-commit: false     # 수동 커밋으로 신뢰성
ack-mode: manual_immediate    # 즉시 커밋
```

### 3. JVM 튜닝
```bash
# Kafka 브로커 JVM 설정
KAFKA_HEAP_OPTS="-Xmx1G -Xms1G"
KAFKA_JVM_PERFORMANCE_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=20"
```

---

## 📈 모니터링

### 1. 토픽 모니터링
```bash
# 전체 상태 확인
bash scripts/kafka/monitor-topics.sh

# 실시간 모니터링
bash scripts/kafka/monitor-topics.sh real-time

# Consumer Lag 확인
bash scripts/kafka/monitor-topics.sh lag
```

### 2. Kafdrop UI
- **접속**: http://localhost:9000
- **기능**: 토픽 목록, 메시지 확인, Consumer Group 상태

### 3. 주요 모니터링 메트릭
- **Producer 메트릭**:
  - `kafka.producer.record-send-rate`: 메시지 전송 속도
  - `kafka.producer.record-error-rate`: 전송 실패율
  - `kafka.producer.batch-size-avg`: 평균 배치 크기

- **Consumer 메트릭**:
  - `kafka.consumer.records-consumed-rate`: 메시지 소비 속도
  - `kafka.consumer.records-lag`: Consumer Lag
  - `kafka.consumer.commit-rate`: 커밋 속도

### 4. 알람 설정
```yaml
alert:
  kafka:
    monitoring:
      alert-slow-processing-ms: 5000    # 5초 이상 처리 시 알람
      max-acceptable-lag: 1000          # 1000개 이상 Lag 시 알람
```

---

## 🔧 운영 가이드

### 1. 시작/종료
```bash
# Kafka 환경 시작
bash scripts/kafka/start-kafka.sh

# Kafka 환경 종료 (데이터 보존)
bash scripts/kafka/stop-kafka.sh

# 완전 정리 (데이터 삭제)
bash scripts/kafka/stop-kafka.sh --clean
```

### 2. 토픽 관리
```bash
# 토픽 목록 확인
kafka-topics.sh --list --bootstrap-server localhost:9092

# 토픽 상세 정보
kafka-topics.sh --describe --topic alert-created-topic --bootstrap-server localhost:9092

# 파티션 증가 (주의: 감소 불가)
kafka-topics.sh --alter --topic user-alert-topic --partitions 20 --bootstrap-server localhost:9092
```

### 3. Consumer Group 관리
```bash
# Consumer Group 목록
kafka-consumer-groups.sh --list --bootstrap-server localhost:9092

# Consumer Group 상태 확인
kafka-consumer-groups.sh --describe --group ssom-alert-consumer --bootstrap-server localhost:9092

# 오프셋 리셋 (주의: 데이터 재처리)
kafka-consumer-groups.sh --reset-offsets --group ssom-alert-consumer --topic user-alert-topic --to-earliest --bootstrap-server localhost:9092 --execute
```

### 4. 메시지 확인
```bash
# 메시지 실시간 확인
kafka-console-consumer.sh --topic user-alert-topic --bootstrap-server localhost:9092 --from-beginning

# 메시지 전송 테스트
kafka-console-producer.sh --topic alert-created-topic --bootstrap-server localhost:9092
```

---

## 🚨 트러블슈팅

### 1. 일반적인 문제

#### Consumer Lag 증가
**증상**: Consumer가 Producer보다 느려서 메시지가 쌓임
**해결방법**:
```bash
# Consumer 스레드 수 증가
spring.kafka.listener.concurrency: 20

# 배치 크기 조정
spring.kafka.consumer.max-poll-records: 5

# Consumer 인스턴스 추가 (스케일 아웃)
```

#### 메시지 처리 실패
**증상**: 특정 메시지에서 계속 실패하여 Consumer가 멈춤
**해결방법**:
```java
// Dead Letter Queue 구현
@KafkaListener(topics = "user-alert-topic")
public void handleUserAlert(UserAlertEvent event, Acknowledgment ack) {
    try {
        processUserAlert(event);
        ack.acknowledge();
    } catch (Exception e) {
        log.error("Processing failed: {}", e.getMessage());
        sendToDeadLetterQueue(event);
        ack.acknowledge(); // 실패해도 커밋하여 블로킹 방지
    }
}
```

#### Kafka 연결 실패
**증상**: `org.apache.kafka.common.errors.TimeoutException`
**해결방법**:
```yaml
spring:
  kafka:
    producer:
      request-timeout-ms: 30000
      delivery-timeout-ms: 120000
    consumer:
      session-timeout: 30000
      heartbeat-interval: 3000
```

### 2. 성능 문제

#### 처리량 부족
**해결방법**:
1. **Consumer 스레드 증가**: `concurrency: 20`
2. **파티션 추가**: 토픽 파티션 수 증가
3. **배치 크기 최적화**: `max-poll-records` 조정
4. **Consumer 인스턴스 추가**: 수평 확장

#### 메모리 부족
**해결방법**:
```yaml
spring:
  kafka:
    producer:
      buffer-memory: 67108864    # 64MB로 증가
      batch-size: 32768          # 32KB로 증가
    consumer:
      fetch-min-size: 10240      # 10KB로 증가
```

### 3. 데이터 일관성 문제

#### 중복 처리
**원인**: Consumer 재시작 시 오프셋 미커밋 메시지 재처리
**해결방법**:
```java
// 멱등성 보장
@Transactional
public void processUserAlert(UserAlertEvent event) {
    // 중복 처리 방지
    if (alertStatusRepository.existsByAlertIdAndUserId(
            event.getAlertId(), event.getUserId())) {
        log.info("Already processed: alertId={}, userId={}", 
                event.getAlertId(), event.getUserId());
        return;
    }
    
    // 실제 처리 로직
    createAlertStatus(event);
}
```

#### 메시지 순서 문제
**해결방법**: 같은 파티션에 순서 보장이 필요한 메시지 전송
```java
// 사용자별 파티셔닝으로 순서 보장
kafkaTemplate.send("user-alert-topic", event.getUserId(), event);
```

---

## 📚 추가 자료

### 관련 문서
- [Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Spring Kafka 문서](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Alert 성능 테스트 결과](../performance/PERFORMANCE_TEST_RESULTS.md)

### 유용한 명령어 모음
```bash
# 개발 환경 전체 시작
bash scripts/kafka/start-kafka.sh

# 토픽 모니터링
bash scripts/kafka/monitor-topics.sh real-time

# 로그 확인
docker-compose -f docker/kafka/docker-compose.kafka.yml logs -f kafka

# 환경 정리
bash scripts/kafka/stop-kafka.sh --clean
```

---

## 🤝 기여하기

개선사항이나 이슈가 있다면 언제든 PR을 보내주세요!

### 주요 개선 포인트
1. **Dead Letter Queue 구현**
2. **Circuit Breaker 패턴 적용**
3. **메트릭 수집 강화**
4. **자동 스케일링 구현**
5. **보안 설정 추가** (SASL/SSL)

---

> 💡 **팁**: Kafdrop UI(http://localhost:9000)를 통해 토픽과 메시지를 시각적으로 확인할 수 있습니다!

---

## 📞 문의

Alert Kafka 시스템 관련 문의사항이 있으시면 팀 채널로 연락주세요! 🚀
