# ELK-LogFlow

> Spring Boot 애플리케이션의 로그를 **구조화(JSON) → 수집(Filebeat) → 버퍼링(Kafka) → 정제(Logstash) → 저장·검색(Elasticsearch) → 시각화(Kibana)** 하는 중앙 로그 파이프라인

서버가 여러 대일 때 장애가 나면 서버마다 접속해서 `grep`으로 로그를 뒤져야 합니다. 이 프로젝트는 그 문제를 해결하려고, 모든 애플리케이션 로그를 한곳으로 모아 **필드 단위로 검색·집계**할 수 있게 만드는 파이프라인을 직접 구성한 것입니다.

<br>

## 아키텍처

```mermaid
flowchart LR
    A["Spring Boot<br/>(logback JSON 로그)"] -- "/logs/app.log<br/>(ndjson)" --> B["Filebeat<br/>(수집 에이전트)"]
    B -- "topic: logs-topic" --> C[("Kafka<br/>(버퍼)")]
    C --> D["Logstash<br/>(정제·변환)"]
    D -- "logflow-YYYY.MM.dd" --> E[("Elasticsearch<br/>(저장·검색)")]
    E --> F["Kibana<br/>(조회·시각화)"]
```

| 단계 | 구성 요소 | 역할 |
|---|---|---|
| 생성 | Spring Boot + logback `LogstashEncoder` | 로그 한 건을 JSON 한 줄로 기록 (`level`, `logger_name`, `traceId`, `stack_trace` …) |
| 수집 | Filebeat 8.10 (`filestream` + `ndjson` 파서) | 로그 파일을 tail 해서 JSON을 필드로 펼친 뒤 Kafka로 전송 |
| 버퍼 | Kafka 7.6 (cp-kafka, KRaft) | 수집과 적재를 분리. ES 장애·트래픽 폭주 시에도 로그를 보관 |
| 정제 | Logstash 8.10 | Kafka에서 소비해 수집용 메타 필드를 제거하고 일별 인덱스로 적재 |
| 저장·검색 | Elasticsearch 8.10 | 필드 단위 검색, 집계. 인덱스 템플릿으로 매핑 고정, ILM으로 30일 후 자동 삭제 |
| 시각화 | Kibana 8.10 | Discover 조회. 데이터 뷰는 `kibana-setup` 컨테이너가 자동 생성 |

<br>

## 기술 스택

- **Application**: Java 17, Spring Boot 3.5, Gradle (Kotlin DSL)
- **Logging**: Logback, logstash-logback-encoder
- **Pipeline**: Filebeat, Apache Kafka (KRaft), Logstash, Elasticsearch, Kibana (8.10)
- **Infra**: Docker, Docker Compose, Kubernetes(Filebeat DaemonSet 예시)

<br>

## 설계 포인트

### 1. 애플리케이션에서 처음부터 JSON으로 남긴다
일반 텍스트 로그를 Logstash `grok`으로 파싱하면 정규식 유지보수 부담이 있습니다. 또 여러 줄짜리 stack trace가 이벤트 여러 건으로 쪼개지는 문제도 생깁니다.
애플리케이션이 처음부터 **구조화된 JSON 한 줄**로 남기면 이런 문제가 없습니다.
- 수집 단계는 파싱 없이 필드만 펼치면 됩니다.
- stack trace도 `stack_trace` 필드 하나에 온전히 담깁니다.

### 2. 수집은 경량 에이전트(Filebeat)에 맡긴다
JVM 기반인 Logstash를 앱 서버마다 띄우는 대신, 가벼운 Filebeat를 에이전트로 둡니다.
- Filebeat는 읽은 위치를 registry에 기록합니다. 그래서 **재시작해도 중복·누락 없이** 이어서 수집합니다.
- 롤링된 파일은 `*.gz`로 압축되므로 수집 대상(`*.log`)에 다시 걸리지 않습니다.

### 3. Kafka로 수집과 적재를 분리한다
Filebeat에서 ES로 바로 보내면 다음과 같은 경우 로그가 밀리거나 유실될 수 있습니다.
- ES 장애나 점검
- 배포 직후처럼 로그가 폭증하는 순간

Kafka를 사이에 두면 얻는 것이 세 가지입니다.
- **장애 격리**: ES가 내려가도 로그는 Kafka에 쌓여 있다가, 복구되면 이어서 적재됩니다.
- **부하 완충**: 순간적인 로그 폭증을 Kafka가 흡수하고, Logstash는 자기 속도로 소비합니다.
- **확장성**: 같은 토픽을 다른 컨슈머(알림, 이상 탐지, 데이터 웨어하우스 등)가 함께 구독할 수 있습니다.

### 4. 정제는 Logstash 한곳에서 한다
필드 정리나 변환 규칙을 수집 에이전트마다 흩어 두지 않습니다. Logstash 파이프라인(`logstash/pipeline/logstash.conf`) 한곳에서 관리합니다.

### 5. 인덱스는 일 단위로 나누고, 수명주기는 ILM에 맡긴다
`logflow-YYYY.MM.dd` 일별 인덱스로 나누면 오래된 로그를 **인덱스 단위로 삭제**할 수 있습니다. 문서를 하나씩 지우는 것보다 훨씬 가볍습니다.
- **인덱스 템플릿**(`elasticsearch/index-template.json`): 동적 매핑에 맡기면 같은 필드가 날마다 다른 타입으로 잡힐 수 있습니다. `level`, `traceId`는 `keyword`, `stack_trace`는 `text`로 고정합니다.
- **ILM**(`elasticsearch/ilm-policy.json`): 7일이 지나면 읽기 전용으로 바꾸고 세그먼트를 병합합니다. 30일이 지나면 자동으로 삭제합니다.
- 첫 인덱스가 만들어지기 전에 등록돼야 하므로, `es-setup` 컨테이너가 끝난 뒤에 Logstash가 뜹니다.

### 6. 요청 하나의 로그를 traceId로 묶는다
`TraceIdFilter`가 요청마다 `traceId`를 MDC에 넣습니다. LogstashEncoder는 MDC를 JSON 필드로 그대로 출력하므로, 그 요청에서 남긴 모든 로그에 같은 `traceId`가 붙습니다.
- 앞단에서 `X-Trace-Id` 헤더를 보내면 그 값을 이어받습니다. 그래서 여러 서비스의 로그를 하나의 흐름으로 조회할 수 있습니다.
- 응답 헤더에도 같은 값을 돌려줍니다. 사용자가 오류를 신고할 때 이 값만 알려 주면 바로 찾을 수 있습니다.
- 외부 값은 로그에 그대로 찍히므로, 영숫자와 `-` 8~64자만 받습니다(로그 인젝션 방지).

<br>

## 실제 서비스에서의 활용

| 시나리오 | 방법 |
|---|---|
| **장애 원인 추적** | Kibana에서 `level:ERROR`로 필터링 → `stack_trace`, `logger_name`으로 원인 클래스 확인. 서버에 접속할 필요 없음 |
| **배포 후 이상 감지** | 배포 시각 전후의 ERROR 건수를 비교해 롤백 여부 판단 |
| **에러율 모니터링·알림** | 시간대별 ERROR 집계 대시보드, 임계치를 넘으면 Kibana Alert(Slack/메일) 발송 |
| **MSA 요청 추적** | Kibana에서 `traceId:<값>`으로 검색. 서비스 간에 `X-Trace-Id` 헤더를 전달하면 여러 서비스의 로그를 하나의 요청 흐름으로 조회 |
| **다중 소비** | 같은 Kafka 토픽을 보안 감사, 사용자 행동 분석 등 다른 시스템이 함께 소비 |

<br>

## 실행 방법

**요구사항**: Docker, Docker Compose (메모리 4GB 이상 권장)

```bash
docker compose up -d --build
```

Kafka와 Elasticsearch가 healthy 상태가 될 때까지 1~2분 정도 걸립니다.

```bash
# 정상 로그 / 에러 로그 발생
curl http://localhost:8080/api/success
curl http://localhost:8080/api/error

# traceId 를 직접 지정 (응답 헤더 X-Trace-Id 로도 돌려받음)
curl -i -H "X-Trace-Id: demo-trace-0001" http://localhost:8080/api/success
```

| 확인 대상 | 방법 |
|---|---|
| Kibana | http://localhost:5601 → Discover → 데이터 뷰 `LogFlow logs` |
| Elasticsearch | `curl "http://localhost:9200/logflow-*/_search?q=level:ERROR&pretty"` |
| 템플릿·ILM 적용 | `curl "http://localhost:9200/logflow-*/_ilm/explain?pretty"` |
| Kafka 토픽 | `docker compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic logs-topic --from-beginning --max-messages 5` |
| Logstash 처리량 | `curl "http://localhost:9600/_node/stats/pipelines?pretty"` |

### 로그 예시
```json
{
  "@timestamp": "2026-10-20T14:24:25.394+09:00",
  "message": "에러 발생! 원인=테스트 오류",
  "logger_name": "com.logflow.controller.AccessController",
  "thread_name": "http-nio-8080-exec-2",
  "level": "ERROR",
  "level_value": 40000,
  "traceId": "3f2a9c1e5b7d4e0f8a6c2b1d9e7f0a3c",
  "app": "logflow"
}
```

<br>

## 프로젝트 구조

```
├── src/main/java/com/logflow/
│   ├── controller/AccessController.java   # 로그 발생용 테스트 API
│   └── logging/TraceIdFilter.java         # 요청별 traceId → MDC
├── src/main/resources/logback-spring.xml  # JSON 로그 설정 (기본: 콘솔 + 롤링 파일, k8s: stdout JSON)
├── src/test/java/com/logflow/             # traceId 필터, JSON 로그 형식, 컨트롤러 테스트
├── filebeat.yml                           # 수집 → Kafka 전송
├── logstash/pipeline/logstash.conf        # Kafka 소비 → 정제 → ES 적재
├── elasticsearch/                         # 인덱스 템플릿, ILM 정책, 등록 스크립트
├── docker-compose.yml                     # 로컬 데모: 전체 파이프라인 단일 호스트 구성
├── deploy/                                # 운영 기준 구성 (보안·다중화·배포 분리, k8s)
└── Dockerfile                             # 멀티스테이지 빌드 (JDK 17 → JRE 17)
```

<br>

## 트러블슈팅

| 문제 | 원인 | 해결 |
|---|---|---|
| 애플리케이션 컨텍스트 로딩 실패 | 내용이 빈 `logback-spring.xml` → Logback XML 파싱 오류(`Premature end of file`) | `LogstashEncoder` 기반 설정 작성 |
| Kafka 컨테이너 기동 실패 | `cp-kafka:latest`(8.x)는 ZooKeeper 모드를 지원하지 않음 | 처음엔 `7.6.1`로 고정해 ZooKeeper 모드로 띄움 → 이후 KRaft 모드로 전환해 ZooKeeper 제거 |
| Filebeat 기동 실패 | 바인드 마운트한 설정 파일이 권한 검사(strict perms)에 걸림 | `--strict.perms=false` 실행 옵션 적용 |
| 로그가 수집되지 않음 | 앱은 `/app/logs`에 쓰는데 볼륨은 `/logs`에 마운트됨 | `LOGGING_FILE_NAME=/logs/app.log` 환경변수로 경로 통일 |
| 모든 로그에 불필요한 `APP_NAME` 필드 | `springProperty`를 `context` 범위로 선언 → LogstashEncoder가 컨텍스트 속성까지 출력 | `local` 범위로 변경하고 `customFields`로 `app` 필드만 추가 |
| Filebeat deprecated 경고 | 8.x에서 `log` 입력이 deprecated | `filestream` 입력 + `ndjson` 파서로 교체 |

<br>

## 운영 적용

루트의 `docker-compose.yml`은 단일 호스트 데모입니다. 운영 기준 구성은 [`deploy/`](deploy/README.md)에 따로 두었습니다.

- **보안**: ES 인증과 TLS(HTTP·transport), Kafka SASL_SSL과 ACL, 계정별 최소 권한, 내부망 IP에만 포트 바인딩
- **가용성**: ES 3노드(샤드 3, 복제본 1), Kafka 브로커 3대(RF 3, `min.insync.replicas=2`), Logstash 다중 인스턴스와 persistent queue
- **데이터 수명주기**: 인덱스 템플릿으로 매핑 고정, ILM으로 30일 후 자동 삭제 (데모와 공용)
- **Kafka 모드**: ZooKeeper 없이 KRaft
- **배포 구조**: 앱 서버(앱 + Filebeat), Kafka, ELK를 서버군별로 분리. 쿠버네티스는 Filebeat DaemonSet으로 컨테이너 stdout을 수집
