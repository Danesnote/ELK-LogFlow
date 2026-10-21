package com.logflow.controller;

import com.logflow.logging.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccessController.class)
@Import(TraceIdFilter.class)
class AccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void success는_OK와_traceId_헤더를_돌려준다() throws Exception {
        mockMvc.perform(get("/api/success"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"))
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    void error는_예외를_던진다() {
        // 처리되지 않은 예외는 MockMvc 에서 ServletException 으로 감싸져 올라옴
        assertThatThrownBy(() -> mockMvc.perform(get("/api/error")))
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("테스트 예외");
    }
}
