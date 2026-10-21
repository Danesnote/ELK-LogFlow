#!/usr/bin/env bash
# Kafka 용 TLS 인증서·키스토어와 SASL 계정 파일을 deploy/prod/secrets/kafka 에 만든다.
# (Elasticsearch/Kibana 인증서는 elk/docker-compose.yml 의 setup 컨테이너가 elasticsearch-certutil 로 만든다)
#
# 필요: openssl, keytool(JDK)
# 사용: deploy/prod/scripts/gen-secrets.sh            # .env 를 읽어 생성 (이미 있으면 건너뜀)
#       deploy/prod/scripts/gen-secrets.sh --force    # 다시 생성
set -euo pipefail

PROD_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$PROD_DIR/.env"
OUT="$PROD_DIR/secrets/kafka"
DAYS=825

[[ -f "$ENV_FILE" ]] || { echo ".env 가 없습니다: cp $PROD_DIR/.env.example $ENV_FILE" >&2; exit 1; }
set -a; source "$ENV_FILE"; set +a

for v in KAFKA1_EXTERNAL_HOST KAFKA2_EXTERNAL_HOST KAFKA3_EXTERNAL_HOST \
         KAFKA_KEYSTORE_PASSWORD KAFKA_TRUSTSTORE_PASSWORD \
         KAFKA_ADMIN_PASSWORD KAFKA_FILEBEAT_PASSWORD KAFKA_LOGSTASH_PASSWORD; do
  [[ -n "${!v:-}" ]] || { echo "$v 가 비어 있습니다" >&2; exit 1; }
done

if [[ -f "$OUT/ca.crt" && "${1:-}" != "--force" ]]; then
  echo "이미 생성되어 있습니다 ($OUT). 다시 만들려면 --force"
  exit 0
fi

rm -rf "$OUT" && mkdir -p "$OUT" && cd "$OUT"

echo "[1/5] CA 생성"
openssl req -x509 -newkey rsa:4096 -sha256 -days 3650 -nodes \
  -keyout ca.key -out ca.crt -subj "/CN=logflow-kafka-ca"

echo "[2/5] 브로커 인증서·키스토어 생성"
for i in 1 2 3; do
  ext_var="KAFKA${i}_EXTERNAL_HOST"
  ext_host="${!ext_var}"
  openssl req -newkey rsa:2048 -sha256 -nodes \
    -keyout "kafka$i.key" -out "kafka$i.csr" -subj "/CN=kafka$i"
  # 컨테이너 내부 이름(kafka$i)과 클라이언트가 쓰는 외부 이름 모두 SAN 에 넣어야 호스트명 검증 통과
  printf 'subjectAltName=DNS:kafka%s,DNS:%s\nextendedKeyUsage=serverAuth,clientAuth\n' "$i" "$ext_host" > "kafka$i.ext"
  openssl x509 -req -in "kafka$i.csr" -CA ca.crt -CAkey ca.key -CAcreateserial \
    -days "$DAYS" -sha256 -extfile "kafka$i.ext" -out "kafka$i.crt"
  openssl pkcs12 -export -in "kafka$i.crt" -inkey "kafka$i.key" -certfile ca.crt \
    -name "kafka$i" -out "kafka$i.keystore.p12" -passout "pass:$KAFKA_KEYSTORE_PASSWORD"
  rm -f "kafka$i.csr" "kafka$i.ext"
done

echo "[3/5] 트러스트스토어 생성 (브로커·Logstash 공용)"
keytool -importcert -noprompt -alias logflow-kafka-ca -file ca.crt \
  -keystore truststore.p12 -storetype PKCS12 -storepass "$KAFKA_TRUSTSTORE_PASSWORD"

# cp-kafka 이미지는 비밀번호를 /etc/kafka/secrets 아래 파일 이름으로 받는다
printf '%s' "$KAFKA_KEYSTORE_PASSWORD"   > keystore_creds
printf '%s' "$KAFKA_KEYSTORE_PASSWORD"   > key_creds
printf '%s' "$KAFKA_TRUSTSTORE_PASSWORD" > truststore_creds

echo "[4/5] SASL/PLAIN 계정 파일 생성"
cat > kafka_server_jaas.conf <<EOF
KafkaServer {
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="admin"
  password="$KAFKA_ADMIN_PASSWORD"
  user_admin="$KAFKA_ADMIN_PASSWORD"
  user_filebeat="$KAFKA_FILEBEAT_PASSWORD"
  user_logstash="$KAFKA_LOGSTASH_PASSWORD";
};
EOF

echo "[5/5] 운영 CLI 용 admin 클라이언트 설정 생성 (헬스체크·토픽/ACL 생성에 사용)"
cat > client-admin.properties <<EOF
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="$KAFKA_ADMIN_PASSWORD";
ssl.truststore.location=/etc/kafka/secrets/truststore.p12
ssl.truststore.password=$KAFKA_TRUSTSTORE_PASSWORD
ssl.truststore.type=PKCS12
EOF

# 컨테이너는 다른 uid(cp-kafka: appuser, filebeat/logstash: 1000)로 읽으므로 읽기 권한은 열어 두고,
# 대신 이 디렉터리가 있는 서버 자체의 접근을 제한한다. CA 개인키만 소유자 전용
chmod 644 ./*
chmod 600 ca.key
# CA 개인키는 인증서 추가 발급에만 필요. 서버에 배포하지 말고 별도 보관할 것
echo "완료: $OUT"
echo "  - 브로커 서버: kafka*.keystore.p12, truststore.p12, *_creds, kafka_server_jaas.conf, client-admin.properties"
echo "  - Logstash 서버: truststore.p12"
echo "  - 앱 서버(Filebeat): ca.crt"
echo "  - ca.key 는 배포하지 말고 별도 보관"
