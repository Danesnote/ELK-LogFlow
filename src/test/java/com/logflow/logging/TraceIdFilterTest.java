package com.logflow.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static com.logflow.logging.TraceIdFilter.TRACE_ID_HEADER;
import static com.logflow.logging.TraceIdFilter.TRACE_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void 헤더가_없으면_새_traceId를_만들어_MDC와_응답헤더에_넣는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInChain = new AtomicReference<>();

        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                mdcInChain.set(MDC.get(TRACE_ID_KEY));
            }
        });

        assertThat(mdcInChain.get()).isNotBlank().hasSize(32);
        assertThat(response.getHeader(TRACE_ID_HEADER)).isEqualTo(mdcInChain.get());
    }

    @Test
    void 유효한_헤더값은_그대로_이어받는다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TRACE_ID_HEADER, "upstream-trace-1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TRACE_ID_HEADER)).isEqualTo("upstream-trace-1234");
    }

    @Test
    void 허용되지_않는_문자가_섞인_헤더값은_버리고_새로_만든다() {
        assertThat(TraceIdFilter.resolveTraceId("abc\n{\"level\":\"ERROR\"}"))
                .doesNotContain("\n")
                .hasSize(32);
    }

    @Test
    void 요청이_끝나면_MDC에서_traceId를_제거한다() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(MDC.get(TRACE_ID_KEY)).isNull();
    }
}
