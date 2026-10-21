package com.logflow.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 요청마다 traceId 를 MDC 에 넣는다.
 * LogstashEncoder 는 MDC 를 JSON 필드로 그대로 출력하므로, 이 요청에서 남긴 모든 로그에 "traceId" 필드가 붙는다.
 * <p>
 * 앞단(게이트웨이, 다른 서비스)이 X-Trace-Id 헤더를 보내면 그 값을 이어받고, 없으면 새로 만든다.
 * 응답 헤더에도 실어 보내서 클라이언트·다음 서비스가 같은 값으로 로그를 찾을 수 있게 한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_KEY = "traceId";

    // 외부에서 들어온 값은 로그에 그대로 찍히므로 길이·문자를 제한 (로그 인젝션 방지)
    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 톰캣 스레드는 재사용되므로 반드시 제거
            MDC.remove(TRACE_ID_KEY);
        }
    }

    static String resolveTraceId(String incoming) {
        if (StringUtils.hasText(incoming) && VALID_TRACE_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
