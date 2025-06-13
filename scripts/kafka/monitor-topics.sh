#!/bin/bash

# Kafka 토픽 모니터링 스크립트
# Alert 시스템 토픽들의 상태를 모니터링합니다.

set -e

# 설정 변수
KAFKA_HOST=${KAFKA_HOST:-localhost:9092}
TOPICS=("alert-created-topic" "user-alert-topic")

echo "📊 Kafka 토픽 모니터링 시작..."
echo "Kafka Host: $KAFKA_HOST"
echo "모니터링 대상 토픽: ${TOPICS[*]}"
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 토픽 존재 여부 확인
check_topics_exist() {
    echo "🔍 토픽 존재 여부 확인..."
    for topic in "${TOPICS[@]}"; do
        if kafka-topics.sh --bootstrap-server $KAFKA_HOST --list | grep -q "^$topic$"; then
            echo -e "  ✅ ${GREEN}$topic${NC} - 존재함"
        else
            echo -e "  ❌ ${RED}$topic${NC} - 존재하지 않음"
        fi
    done
    echo ""
}

# 토픽 상세 정보
show_topic_details() {
    echo "📋 토픽 상세 정보..."
    for topic in "${TOPICS[@]}"; do
        if kafka-topics.sh --bootstrap-server $KAFKA_HOST --list | grep -q "^$topic$"; then
            echo -e "${BLUE}=== $topic ===${NC}"
            kafka-topics.sh --bootstrap-server $KAFKA_HOST --describe --topic $topic
            echo ""
        fi
    done
}

# Consumer Group 상태 확인
check_consumer_groups() {
    echo "👥 Consumer Group 상태 확인..."
    
    # Consumer Group 목록
    groups=$(kafka-consumer-groups.sh --bootstrap-server $KAFKA_HOST --list | grep ssom || true)
    
    if [ -z "$groups" ]; then
        echo -e "  ⚠️  ${YELLOW}활성 Consumer Group이 없습니다${NC}"
    else
        echo "활성 Consumer Groups:"
        echo "$groups" | while read group; do
            echo -e "  📍 ${GREEN}$group${NC}"
            kafka-consumer-groups.sh --bootstrap-server $KAFKA_HOST --describe --group $group
            echo ""
        done
    fi
    echo ""
}

# 토픽별 메시지 수 확인
check_message_count() {
    echo "📈 토픽별 메시지 현황..."
    for topic in "${TOPICS[@]}"; do
        if kafka-topics.sh --bootstrap-server $KAFKA_HOST --list | grep -q "^$topic$"; then
            echo -e "${BLUE}=== $topic 메시지 현황 ===${NC}"
            
            # 파티션별 오프셋 정보
            kafka-run-class.sh kafka.tools.GetOffsetShell \
                --bootstrap-server $KAFKA_HOST \
                --topic $topic \
                --time -1 | \
            while IFS=: read topic partition offset; do
                echo "  파티션 $partition: $offset 메시지"
            done
            echo ""
        fi
    done
}

# Lag 확인
check_consumer_lag() {
    echo "⏱️  Consumer Lag 확인..."
    
    groups=$(kafka-consumer-groups.sh --bootstrap-server $KAFKA_HOST --list | grep ssom || true)
    
    if [ -n "$groups" ]; then
        echo "$groups" | while read group; do
            echo -e "${BLUE}=== $group Lag 정보 ===${NC}"
            kafka-consumer-groups.sh --bootstrap-server $KAFKA_HOST --describe --group $group | \
            awk 'NR>1 && $5 != "-" {
                lag = $5
                if (lag > 1000) {
                    printf "  🔴 %s[%s]: Lag %s (High)\n", $1, $2, lag
                } else if (lag > 100) {
                    printf "  🟡 %s[%s]: Lag %s (Medium)\n", $1, $2, lag
                } else {
                    printf "  🟢 %s[%s]: Lag %s (Low)\n", $1, $2, lag
                }
            }'
            echo ""
        done
    else
        echo -e "  ℹ️  ${BLUE}활성 Consumer가 없어 Lag 정보가 없습니다${NC}"
    fi
    echo ""
}

# 실시간 모니터링 모드
real_time_monitor() {
    echo "🔄 실시간 모니터링 모드 (Ctrl+C로 종료)"
    echo ""
    
    while true; do
        clear
        echo "🕐 $(date '+%Y-%m-%d %H:%M:%S') - Kafka Alert 토픽 실시간 모니터링"
        echo "=" * 70
        
        check_topics_exist
        check_message_count
        check_consumer_lag
        
        echo "다음 업데이트까지 10초 대기... (Ctrl+C로 종료)"
        sleep 10
    done
}

# 메인 실행
main() {
    case "${1:-}" in
        "real-time"|"rt")
            real_time_monitor
            ;;
        "lag")
            check_consumer_lag
            ;;
        "count")
            check_message_count
            ;;
        "groups")
            check_consumer_groups
            ;;
        *)
            check_topics_exist
            show_topic_details
            check_consumer_groups
            check_message_count
            check_consumer_lag
            
            echo "💡 사용법:"
            echo "  $0              # 전체 상태 확인"
            echo "  $0 real-time    # 실시간 모니터링"
            echo "  $0 lag          # Consumer Lag만 확인"
            echo "  $0 count        # 메시지 수만 확인"
            echo "  $0 groups       # Consumer Group만 확인"
            ;;
    esac
}

# 스크립트 실행
main "$@"
