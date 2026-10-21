#!/usr/bin/env bash
# logs-topic 을 만들고 계정별 최소 권한 ACL 을 부여한다. 이미 있으면 그대로 둔다.
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-kafka1:29092}"
CONFIG=/etc/kafka/secrets/client-admin.properties
TOPIC=logs-topic
GROUP=logstash-logflow

echo "[kafka-init] 토픽 생성: $TOPIC"
# 파티션 수 = Logstash 가 병렬로 소비할 수 있는 최대 인스턴스 수
kafka-topics --bootstrap-server "$BOOTSTRAP" --command-config "$CONFIG" \
  --create --if-not-exists --topic "$TOPIC" \
  --partitions 6 --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=259200000

echo "[kafka-init] ACL: filebeat 는 $TOPIC 쓰기만"
kafka-acls --bootstrap-server "$BOOTSTRAP" --command-config "$CONFIG" \
  --add --allow-principal User:filebeat \
  --producer --topic "$TOPIC"

echo "[kafka-init] ACL: logstash 는 $TOPIC 읽기 + $GROUP 컨슈머 그룹만"
kafka-acls --bootstrap-server "$BOOTSTRAP" --command-config "$CONFIG" \
  --add --allow-principal User:logstash \
  --consumer --topic "$TOPIC" --group "$GROUP"

kafka-topics --bootstrap-server "$BOOTSTRAP" --command-config "$CONFIG" --describe --topic "$TOPIC"
echo "[kafka-init] 완료"
