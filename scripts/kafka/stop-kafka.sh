#!/bin/bash

# Kafka 환경 종료 스크립트
# Docker Compose를 사용하여 Kafka 클러스터를 종료합니다.

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

echo -e "${BLUE}🛑 SSOM Alert Kafka 환경 종료${NC}"
echo "프로젝트 루트: $PROJECT_ROOT"
echo "Docker Compose 파일: $KAFKA_COMPOSE_FILE"
echo ""

# Docker Compose 파일 존재 확인
if [ ! -f "$KAFKA_COMPOSE_FILE" ]; then
    echo -e "${RED}❌ Docker Compose 파일을 찾을 수 없습니다: $KAFKA_COMPOSE_FILE${NC}"
    exit 1
fi

# 실행 중인 컨테이너 확인
echo -e "${BLUE}📋 현재 실행 중인 Kafka 컨테이너:${NC}"
docker-compose -f "$KAFKA_COMPOSE_FILE" ps

echo ""

# 종료 옵션 확인
CLEAN_VOLUMES=false
if [ "${1:-}" = "--clean" ] || [ "${1:-}" = "-c" ]; then
    CLEAN_VOLUMES=true
    echo -e "${YELLOW}🧹 볼륨도 함께 삭제됩니다 (데이터 완전 삭제)${NC}"
    read -p "계속하시겠습니까? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}ℹ️  종료가 취소되었습니다.${NC}"
        exit 0
    fi
fi

# Kafka 클러스터 종료
echo -e "${YELLOW}📦 Kafka 클러스터 종료 중...${NC}"

if [ "$CLEAN_VOLUMES" = true ]; then
    # 볼륨까지 삭제
    docker-compose -f "$KAFKA_COMPOSE_FILE" down -v --remove-orphans
    echo -e "${GREEN}✅ Kafka 클러스터와 모든 데이터가 삭제되었습니다.${NC}"
else
    # 컨테이너만 종료 (데이터 보존)
    docker-compose -f "$KAFKA_COMPOSE_FILE" down --remove-orphans
    echo -e "${GREEN}✅ Kafka 클러스터가 종료되었습니다. (데이터는 보존됨)${NC}"
fi

# 종료 후 상태 확인
echo ""
echo -e "${BLUE}📊 종료 후 컨테이너 상태:${NC}"
if docker-compose -f "$KAFKA_COMPOSE_FILE" ps | grep -q "Up"; then
    echo -e "${YELLOW}⚠️  일부 컨테이너가 아직 실행 중입니다:${NC}"
    docker-compose -f "$KAFKA_COMPOSE_FILE" ps
else
    echo -e "${GREEN}✅ 모든 Kafka 컨테이너가 성공적으로 종료되었습니다.${NC}"
fi

# 사용 중인 포트 확인
echo ""
echo -e "${BLUE}🔍 포트 사용 상태 확인:${NC}"
PORTS=(2181 9092 9000 8081)
for port in "${PORTS[@]}"; do
    if lsof -i :$port >/dev/null 2>&1; then
        echo -e "  • 포트 ${port}: ${YELLOW}사용 중${NC}"
    else
        echo -e "  • 포트 ${port}: ${GREEN}사용 가능${NC}"
    fi
done

echo ""
echo -e "${GREEN}🎉 Kafka 환경 종료가 완료되었습니다!${NC}"
echo ""
echo "💡 다시 시작하려면:"
echo "  bash $SCRIPT_DIR/start-kafka.sh"
echo ""
if [ "$CLEAN_VOLUMES" = false ]; then
    echo "🗑️  데이터까지 완전히 삭제하려면:"
    echo "  bash $SCRIPT_DIR/stop-kafka.sh --clean"
    echo ""
fi
