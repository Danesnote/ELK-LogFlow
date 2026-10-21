package com.logflow.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * logback-spring.xml 의 JSON_FILE 인코더와 같은 설정으로 한 줄을 만들어,
 * Filebeat ndjson 파서와 ES 인덱스 템플릿이 기대하는 필드가 나오는지 확인한다.
 */
class JsonLogFormatTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LoggerContext context = new LoggerContext();
    private LogstashEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new LogstashEncoder();
        encoder.setContext(context);
        encoder.setCustomFields("{\"app\":\"logflow\"}");
        encoder.start();
    }

    @AfterEach
    void tearDown() {
        encoder.stop();
    }

    @Test
    void 로그_한_건이_JSON_한_줄로_필요한_필드를_모두_담는다() throws Exception {
        Logger logger = context.getLogger("com.logflow.controller.AccessController");
        LoggingEvent event = new LoggingEvent(Logger.FQCN, logger, Level.ERROR, "에러 발생!",
                new IllegalStateException("boom"), null);
        event.setMDCPropertyMap(Map.of("traceId", "abc123def456"));

        String line = new String(encoder.encode(event), StandardCharsets.UTF_8);

        // ndjson: 끝의 개행 하나를 빼면 줄바꿈이 없어야 함 (stack trace 포함)
        assertThat(line.stripTrailing()).doesNotContain("\n");

        JsonNode json = objectMapper.readTree(line);
        assertThat(json.has("@timestamp")).isTrue();
        assertThat(json.get("level").asText()).isEqualTo("ERROR");
        assertThat(json.get("message").asText()).isEqualTo("에러 발생!");
        assertThat(json.get("logger_name").asText()).isEqualTo("com.logflow.controller.AccessController");
        assertThat(json.get("traceId").asText()).isEqualTo("abc123def456");
        assertThat(json.get("app").asText()).isEqualTo("logflow");
        assertThat(json.get("stack_trace").asText()).contains("IllegalStateException: boom");
    }
}
