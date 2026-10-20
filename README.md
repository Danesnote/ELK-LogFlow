[Web/App Servers] --(Filebeat)--> [Kafka Cluster] --> [Logstash Cluster] --> [Elasticsearch Cluster] --> [Kibana]

## 현재 구성

```
Spring Boot ──(JSON 로그: ./logs/app.log)──> Filebeat ──> Elasticsearch ──> Kibana
```

- 앱은 `logback-spring.xml`의 `LogstashEncoder`로 한 줄에 JSON 하나씩(ndjson) 남깁니다.
- Filebeat(`filestream` + `ndjson` 파서)가 이 로그를 `level`, `logger_name`, `stack_trace` 등의 필드로 펼쳐 Elasticsearch로 보냅니다.
- Kafka/ZooKeeper는 `kafka` 프로필에서만 뜹니다(아직 Filebeat는 ES로 직접 전송).

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

- Kibana: http://localhost:5601 → Discover → 데이터 뷰 `LogFlow logs` (`filebeat-*`, `kibana-setup` 컨테이너가 자동으로 생성)
- ES에서 직접 확인: `curl "http://localhost:9200/filebeat-*/_search?q=level:ERROR&pretty"`
- Kafka까지 띄우기: `docker compose --profile kafka up -d`
