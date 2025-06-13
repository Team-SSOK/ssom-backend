#!/bin/bash

# Kafka 토픽 생성 스크립트
# Alert 시스템용 토픽들을 생성합니다.

set -e

# 설정 변수
KAFKA_HOST=${KAFKA_HOST:-localhost:9092}
PARTITIONS=${PARTITIONS:-10}
REPLICATION_FACTOR=${REPLICATION_FACTOR:-1}
RETENTION_MS=${RETENTION_MS:-604800000}  # 7일 (7 * 24 * 60 * 60 * 1000)

echo "🚀 Kafka 토픽 생성 시작..."
echo "Kafka Host: $KAFKA_HOST"
echo "Partitions: $PARTITIONS"
echo "Replication Factor: $REPLICATION_FACTOR"
echo "Retention: $RETENTION_MS ms"
echo ""

# 1. alert-created-topic 생성
echo "📢 alert-created-topic 생성 중..."
kafka-topics.sh --create \
  --topic alert-created-topic \
  --bootstrap-server $KAFKA_HOST \
  --partitions 3 \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=$RETENTION_MS \
  --config compression.type=lz4 \
  --config cleanup.policy=delete \
  --if-not-exists

echo "✅ alert-created-topic 생성 완료"

# 2. user-alert-topic 생성
echo "👤 user-alert-topic 생성 중..."
kafka-topics.sh --create \
  --topic user-alert-topic \
  --bootstrap-server $KAFKA_HOST \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config retention.ms=$RETENTION_MS \
  --config compression.type=lz4 \
  --config cleanup.policy=delete \
  --if-not-exists

echo "✅ user-alert-topic 생성 완료"

# 토픽 목록 확인
echo ""
echo "📋 생성된 토픽 목록:"
kafka-topics.sh --list --bootstrap-server $KAFKA_HOST | grep -E "(alert-created-topic|user-alert-topic)"

# 토픽 상세 정보
echo ""
echo "📊 토픽 상세 정보:"
echo "--- alert-created-topic ---"
kafka-topics.sh --describe --topic alert-created-topic --bootstrap-server $KAFKA_HOST

echo ""
echo "--- user-alert-topic ---"
kafka-topics.sh --describe --topic user-alert-topic --bootstrap-server $KAFKA_HOST

echo ""
echo "🎉 모든 토픽 생성이 완료되었습니다!"
