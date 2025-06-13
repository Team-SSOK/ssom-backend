# 🚀 Kafka TopicBuilder를 통한 토픽 자동 생성

## 📋 개요

Spring Kafka의 `TopicBuilder`를 사용하여 애플리케이션 시작 시 **토픽을 자동으로 생성**하는 방식입니다. 별도의 스크립트 실행 없이 Spring Bean으로 토픽을 정의하면 자동으로 생성됩니다.

---

## 🎯 **스크립트 vs TopicBuilder 비교**

| 측면 | 스크립트 방식 | TopicBuilder 방식 |
|------|---------------|-------------------|
| **실행 시점** | 수동 실행 필요 | 애플리케이션 시작 시 자동 ✅ |
| **설정 관리** | 별도 스크립트 파일 | application.yml 통합 ✅ |
| **환경별 관리** | 환경변수로 분기 | Spring Profile 자동 적용 ✅ |
| **버전 관리** | 스크립트 파일 별도 관리 | 소스코드와 함께 관리 ✅ |
| **에러 처리** | 스크립트 실행 실패 시 수동 대응 | Spring 컨텍스트 로딩 실패로 명확한 에러 ✅ |
| **테스트** | 별도 테스트 환경 구성 | 단위 테스트로 간단히 검증 ✅ |
| **CI/CD** | 파이프라인에 스크립트 실행 단계 추가 | 애플리케이션 배포만으로 완료 ✅ |

---

## 🚀 **실행 방법**

### 1. **로컬 개발 환경**
```bash
# 1. Kafka 시작 (Docker)
bash scripts/kafka/start-kafka.sh

# 2. 애플리케이션 시작 (토픽 자동 생성됨)
./gradlew bootRun --args='--spring.profiles.active=dev'

# 3. 토픽 생성 확인
bash scripts/kafka/monitor-topics.sh
```

### 2. **운영 환경**
```bash
# 1. 운영 Kafka 클러스터 준비

# 2. 환경변수 설정
export KAFKA_BOOTSTRAP_SERVERS=prod-kafka-cluster:9092
export KAFKA_REPLICATION_FACTOR=3
export USER_ALERT_PARTITIONS=20

# 3. 애플리케이션 배포 (토픽 자동 생성됨)
java -jar ssom-backend.jar --spring.profiles.active=prod
```

---

## 🎉 **결론**

### ✅ **TopicBuilder 방식의 장점**
- ⚡ **자동 생성**: 애플리케이션 시작 시 자동으로 토픽 생성
- 🔧 **코드로 관리**: 토픽 설정을 코드로 버전 관리
- 🌍 **환경별 설정**: application.yml 프로파일별 설정 자동 적용
- 🛡️ **안전성**: 코드 리뷰를 통한 토픽 설정 검증
- 📦 **일관성**: 개발/테스트/운영 환경에서 동일한 방식으로 토픽 생성

### 🔄 **언제 어떤 방식을 사용할까?**

#### **TopicBuilder 추천 상황**
- 🆕 **새로운 프로젝트**: 처음부터 자동화된 토픽 관리
- 👥 **팀 온보딩**: 새로운 개발자가 쉽게 환경 구성
- 🔄 **CI/CD 자동화**: 복잡한 배포 스크립트 제거
- 🧪 **테스트 자동화**: 단위 테스트에서 토픽 설정 검증

#### **스크립트 방식이 필요한 상황**
- 🏢 **기존 운영 환경**: 이미 스크립트로 관리되는 환경
- 🔧 **복잡한 토픽 관리**: 동적 토픽 생성이나 특수한 설정
- 🛠️ **운영 툴링**: Kafka 관리 도구와의 연동
- 🔍 **디버깅**: 토픽 생성 과정의 세밀한 제어

---

## 💡 **최종 권장사항**

### **SSOM Alert 프로젝트**
1. **개발 환경**: TopicBuilder 사용 (자동화)
2. **운영 환경**: TopicBuilder 사용 (안정성)
3. **모니터링**: 기존 스크립트 활용 (monitor-topics.sh)
4. **긴급 상황**: 스크립트 백업 (create-topics.sh)

### **최적의 접근법**
```bash
# 🎯 권장: TopicBuilder 방식 사용
./gradlew bootRun --args='--spring.profiles.active=dev'

# 🔄 백업: 필요시 스크립트 사용
bash scripts/kafka/create-topics.sh

# 📊 모니터링: 스크립트 활용
bash scripts/kafka/monitor-topics.sh real-time
```

**TopicBuilder 방식으로 개발 생산성과 운영 안정성을 모두 확보하세요!** 🚀✨
