#!/usr/bin/env bash
# Elastic Stack 운영 초기화 (elasticsearch 이미지에서 root 로 실행)
#   1) CA·노드 인증서 발급 (최초 1회)            → es01~03 은 이 단계가 끝나야 기동
#   2) ES 클러스터가 뜨면 kibana_system 비밀번호 설정
#   3) Logstash 쓰기 전용 역할·계정 생성
#   4) ILM 정책 + 인덱스 템플릿 등록 (루트 elasticsearch/setup.sh 재사용)
set -euo pipefail

CERTS=/usr/share/elasticsearch/config/certs
ES_URL=https://es01:9200
CA="$CERTS/ca/ca.crt"

for v in ELASTIC_PASSWORD KIBANA_PASSWORD LOGSTASH_ES_PASSWORD; do
  [[ -n "${!v:-}" ]] || { echo "$v 가 비어 있습니다" >&2; exit 1; }
done

# ---------- 1) 인증서 ----------
if [[ ! -f "$CERTS/ca.zip" ]]; then
  echo "[setup] CA 생성"
  bin/elasticsearch-certutil ca --silent --pem -out "$CERTS/ca.zip"
  unzip -o "$CERTS/ca.zip" -d "$CERTS"
fi
if [[ ! -f "$CERTS/certs.zip" ]]; then
  echo "[setup] 노드 인증서 생성"
  bin/elasticsearch-certutil cert --silent --pem \
    --in /setup/instances.yml \
    --ca-cert "$CA" --ca-key "$CERTS/ca/ca.key" \
    -out "$CERTS/certs.zip"
  unzip -o "$CERTS/certs.zip" -d "$CERTS"
fi
chown -R root:root "$CERTS"
find "$CERTS" -type d -exec chmod 750 {} \;
find "$CERTS" -type f -exec chmod 640 {} \;
# CA 인증서(공개 정보)는 root 그룹이 아닌 Logstash(uid 1000)도 읽을 수 있게
chmod 755 "$CERTS" "$CERTS/ca"
chmod 644 "$CA"
# 헬스체크가 이 파일을 보고 ES 노드 기동을 허용함
touch "$CERTS/.ready"

# ---------- 2) ES 대기 ----------
es() { curl -sS --cacert "$CA" -u "elastic:$ELASTIC_PASSWORD" -H "Content-Type: application/json" "$@"; }

echo "[setup] ES 클러스터 대기"
until es "$ES_URL/_cluster/health?wait_for_status=yellow&timeout=10s" | grep -q '"status"'; do sleep 5; done

# ---------- 3) 내장 계정 / Logstash 계정 ----------
echo "[setup] kibana_system 비밀번호 설정"
until es -X POST "$ES_URL/_security/user/kibana_system/_password" \
        -d "{\"password\":\"$KIBANA_PASSWORD\"}" | grep -q '^{}'; do sleep 5; done

echo "[setup] logstash_writer 역할 생성 (logflow-* 쓰기만)"
es -X PUT "$ES_URL/_security/role/logstash_writer" -d '{
  "cluster": ["monitor"],
  "indices": [{
    "names": ["logflow-*"],
    "privileges": ["create_doc", "create_index", "view_index_metadata"]
  }]
}'
echo

echo "[setup] logstash_internal 계정 생성"
es -X PUT "$ES_URL/_security/user/logstash_internal" -d "{
  \"password\": \"$LOGSTASH_ES_PASSWORD\",
  \"roles\": [\"logstash_writer\"],
  \"full_name\": \"Logstash writer for logflow\"
}"
echo

echo "[setup] Kibana 조회 전용 역할 생성 (logflow_reader: 개발자 계정에 부여)"
es -X PUT "$ES_URL/_security/role/logflow_reader" -d '{
  "indices": [{ "names": ["logflow-*"], "privileges": ["read", "view_index_metadata"] }],
  "applications": [{
    "application": "kibana-.kibana",
    "privileges": ["feature_discover.read", "feature_dashboard.read", "feature_visualize.read"],
    "resources": ["*"]
  }]
}'
echo

# ---------- 4) ILM + 인덱스 템플릿 ----------
ES_URL="$ES_URL" ES_AUTH="elastic:$ELASTIC_PASSWORD" ES_CACERT="$CA" \
  ILM_FILE=/setup-index/ilm-policy.json \
  TEMPLATE_FILE=/setup-prod-index/index-template.json \
  sh /setup-index/setup.sh

echo "[setup] 완료"
