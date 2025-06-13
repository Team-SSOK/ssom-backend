#!/bin/bash

# Kafka 환경 시작 스크립트
# Docker Compose를 사용하여 Kafka 클러스터를 시작합니다.

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 프로젝트 루트 디렉토리로 이동
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
KAFKA_COMPOSE_FILE="$PROJECT_ROOT/docker/kafka/docker-compose.kafka.yml"

echo -e "${BLUE}🚀 SSOM Alert Kafka 환경 시작${NC}"
echo "프로젝트 루트: $PROJECT_ROOT"
echo "Docker Compose 파일: $KAFKA_COMPOSE_FILE"
echo ""

# Docker Compose 파일 존재 확인
if [ ! -f "$KAFKA_COMPOSE_FILE" ]; then
    echo -e "${RED}❌ Docker Compose 파일을 찾을 수 없습니다: $KAFKA_COMPOSE_FILE${NC}"
    exit 1
fi

# Docker가 실행 중인지 확인
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker가 실행되지 않았습니다. Docker를 시작해주세요.${NC}"
    exit 1
fi

# 기존 컨테이너 정리 (선택사항)
if [ "${1:-}" = "--clean" ]; then
    echo -e "${YELLOW}🧹 기존 Kafka 컨테이너 정리 중...${NC}"
    docker-compose -f "$KAFKA_COMPOSE_FILE" down -v --remove-orphans
    echo ""
fi

# Kafka 클러스터 시작
echo -e "${BLUE}📦 Kafka 클러스터 시작 중...${NC}"
docker-compose -f "$KAFKA_COMPOSE_FILE" up -d

echo ""
echo -e "${YELLOW}⏳ Kafka 클러스터 준비 대기 중...${NC}"

# Kafka 준비 상태 확인
check_kafka_ready() {
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec ssom-kafka kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    return 1
}

if check_kafka_ready; then
    echo ""
    echo -e "${GREEN}✅ Kafka 클러스터가 준비되었습니다!${NC}"
else
    echo ""
    echo -e "${RED}❌ Kafka 클러스터 시작에 실패했습니다.${NC}"
    echo "로그 확인: docker-compose -f $KAFKA_COMPOSE_FILE logs"
    exit 1
fi

# 토픽 자동 생성
echo ""
echo -e "${BLUE}📢 Alert 토픽 생성 중...${NC}"
if [ -f "$SCRIPT_DIR/create-topics.sh" ]; then
    bash "$SCRIPT_DIR/create-topics.sh"
else
    echo -e "${YELLOW}⚠️  토픽 생성 스크립트를 찾을 수 없습니다. 수동으로 생성해주세요.${NC}"
fi

# 서비스 상태 출력
echo ""
echo -e "${GREEN}🎉 Kafka 환경이 성공적으로 시작되었습니다!${NC}"
echo ""
echo "📊 서비스 접속 정보:"
echo "  • Kafka Broker: localhost:9092"
echo "  • Zookeeper: localhost:2181"
echo "  • Kafdrop UI: http://localhost:9000"
echo "  • Schema Registry: http://localhost:8081"
echo ""
echo "💡 유용한 명령어:"
echo "  • 상태 확인: docker-compose -f $KAFKA_COMPOSE_FILE ps"
echo "  • 로그 보기: docker-compose -f $KAFKA_COMPOSE_FILE logs -f"
echo "  • 토픽 모니터링: bash $SCRIPT_DIR/monitor-topics.sh"
echo "  • 환경 종료: bash $SCRIPT_DIR/stop-kafka.sh"
echo ""

# 컨테이너 상태 표시
echo -e "${BLUE}📋 컨테이너 상태:${NC}"
docker-compose -f "$KAFKA_COMPOSE_FILE" ps
