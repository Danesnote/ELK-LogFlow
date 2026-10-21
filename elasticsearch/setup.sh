#!/bin/sh
# ILM 정책과 인덱스 템플릿을 ES 에 등록하고 종료하는 1회성 스크립트.
# 첫 인덱스(logflow-YYYY.MM.dd)가 만들어지기 전에 실행되어야 매핑·ILM 이 적용되므로
# docker-compose 에서 logstash 가 이 컨테이너의 완료를 기다린다.
#
# 사용: ES_URL=http://elasticsearch:9200 [ES_AUTH=user:password] [ES_CACERT=/certs/ca.crt] \
#       [ILM_FILE=...] [TEMPLATE_FILE=...] setup.sh
set -eu

ES_URL="${ES_URL:-http://elasticsearch:9200}"
DIR="$(cd "$(dirname "$0")" && pwd)"
ILM_FILE="${ILM_FILE:-$DIR/ilm-policy.json}"
TEMPLATE_FILE="${TEMPLATE_FILE:-$DIR/index-template.json}"

curl_es() {
  # $1=method $2=path [$3=json file]
  set -- "$1" "$2" "${3:-}"
  opts="-sS -f -X $1"
  [ -n "${ES_AUTH:-}" ] && opts="$opts -u $ES_AUTH"
  [ -n "${ES_CACERT:-}" ] && opts="$opts --cacert $ES_CACERT"
  if [ -n "$3" ]; then
    # shellcheck disable=SC2086
    curl $opts -H "Content-Type: application/json" "$ES_URL$2" --data-binary "@$3"
  else
    # shellcheck disable=SC2086
    curl $opts "$ES_URL$2"
  fi
  echo
}

echo "[setup] ILM 정책 등록: logflow-policy"
curl_es PUT "/_ilm/policy/logflow-policy" "$ILM_FILE"

echo "[setup] 인덱스 템플릿 등록: logflow"
curl_es PUT "/_index_template/logflow" "$TEMPLATE_FILE"

echo "[setup] 완료"
