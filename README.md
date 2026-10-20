[Web/App Servers] --(Filebeat)--> [Kafka Cluster] --> [Logstash Cluster] --> [Elasticsearch Cluster] --> [Kibana]

## 현재 구성

```
Spring Boot ──(JSON 로그: ./logs/app.log)──> Filebeat ──(logs-topic)──> Kafka ──> Logstash ──> Elasticsearch(logflow-YYYY.MM.dd) ──> Kibana
```

- 앱은 `logback-spring.xml`의 `LogstashEncoder`로 한 줄에 JSON 하나씩(ndjson) 남깁니다.
- Filebeat(`filestream` + `ndjson` 파서)가 이 로그를 `level`, `logger_name`, `stack_trace` 등의 필드로 펼쳐서 Kafka `logs-topic`으로 보냅니다.
- Logstash(`logstash/pipeline/logstash.conf`)가 Kafka에서 소비하고, 수집용 메타 필드를 정리한 뒤 일별 인덱스 `logflow-YYYY.MM.dd`에 적재합니다.

## 실행

```bash
docker compose up -d --build
```

```bash
curl http://localhost:8080/api/success
```

```bash
curl http://localhost:8080/api/error
```

- Kibana: http://localhost:5601 → Discover → 데이터 뷰 `LogFlow logs`(`logflow-*`, `kibana-setup` 컨테이너가 자동으로 생성)
- ES에서 직접 확인: `curl "http://localhost:9200/logflow-*/_search?q=level:ERROR&pretty"`
- Kafka 토픽 확인: `docker compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic logs-topic --from-beginning --max-messages 5`
- Logstash 처리량: `curl "http://localhost:9600/_node/stats/pipelines?pretty"`
