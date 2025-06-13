#!/bin/bash

# Kafka 스크립트 실행 권한 설정
# 모든 Kafka 관련 스크립트에 실행 권한을 부여합니다.

set -e

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트 디렉토리
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}🔧 Kafka 스크립트 실행 권한 설정${NC}"
echo "스크립트 디렉토리: $SCRIPT_DIR"
echo ""

# 실행 권한 부여할 스크립트 목록
SCRIPTS=(
    "start-kafka.sh"
    "stop-kafka.sh"
    "create-topics.sh"
    "monitor-topics.sh"
    "setup-permissions.sh"
)

# 각 스크립트에 실행 권한 부여
for script in "${SCRIPTS[@]}"; do
    script_path="$SCRIPT_DIR/$script"
    if [ -f "$script_path" ]; then
        chmod +x "$script_path"
        echo -e "✅ ${GREEN}$script${NC} - 실행 권한 부여 완료"
    else
        echo -e "⚠️  ${script} - 파일을 찾을 수 없습니다"
    fi
done

echo ""
echo -e "${GREEN}🎉 모든 스크립트 권한 설정이 완료되었습니다!${NC}"
echo ""
echo "📋 사용 가능한 명령어:"
echo "  • Kafka 시작: bash $SCRIPT_DIR/start-kafka.sh"
echo "  • Kafka 종료: bash $SCRIPT_DIR/stop-kafka.sh"
echo "  • 토픽 생성: bash $SCRIPT_DIR/create-topics.sh"
echo "  • 토픽 모니터링: bash $SCRIPT_DIR/monitor-topics.sh"
