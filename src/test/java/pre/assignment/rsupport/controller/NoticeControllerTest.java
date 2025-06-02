package pre.assignment.rsupport.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import pre.assignment.rsupport.domain.Notice;
import pre.assignment.rsupport.dto.NoticeDetailResponseDto;
import pre.assignment.rsupport.dto.NoticeRequestDto;
import pre.assignment.rsupport.service.NoticeService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoticeController.class)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoticeService noticeService;

    @Autowired
    private ObjectMapper objectMapper;

    private Notice notice;
    private NoticeRequestDto noticeRequestDto;

    @BeforeEach
    void setUp() {
        notice = Notice.builder()
                .id(1L)
                .title("테스트 공지사항")
                .content("테스트 내용")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(7))
                .author("테스트 작성자")
                .viewCount(0)
                .build();

        noticeRequestDto = NoticeRequestDto.builder()
                .title("테스트 공지사항")
                .content("테스트 내용")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(7))
                .author("테스트 작성자")
                .build();
    }

    @Test
    void createNotice_성공() throws Exception {
        when(noticeService.createNotice(any(Notice.class), any())).thenReturn(notice);

        String noticeJson = objectMapper.writeValueAsString(noticeRequestDto);
        MockMultipartFile noticePart = new MockMultipartFile(
                "notice",
                "notice",
                MediaType.APPLICATION_JSON_VALUE,
                noticeJson.getBytes()
        );

        mockMvc.perform(multipart("/api/notices")
                        .file(noticePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("테스트 공지사항"));
    }

    @Test
    void updateNotice_성공() throws Exception {
        when(noticeService.updateNotice(eq(1L), any(Notice.class), any())).thenReturn(notice);

        String noticeJson = objectMapper.writeValueAsString(noticeRequestDto);
        MockMultipartFile noticePart = new MockMultipartFile(
                "notice",
                "notice",
                MediaType.APPLICATION_JSON_VALUE,
                noticeJson.getBytes()
        );

        mockMvc.perform(multipart("/api/notices/1")
                        .file(noticePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("테스트 공지사항"));
    }

    @Test
    void deleteNotice_성공() throws Exception {
        mockMvc.perform(delete("/api/notices/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getNotice_성공() throws Exception {
        when(noticeService.getNotice(1L)).thenReturn(notice);

        mockMvc.perform(get("/api/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("테스트 공지사항"));
    }

    @Test
    void searchNotices_성공() throws Exception {
        Page<Notice> noticePage = new PageImpl<>(Collections.singletonList(notice));
        when(noticeService.searchNotices(any(), any(), any(Pageable.class))).thenReturn(noticePage);

        mockMvc.perform(get("/api/notices")
                        .param("searchType", "title")
                        .param("keyword", "테스트")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("테스트 공지사항"));
    }

    @Test
    void searchNoticesWithDate_성공() throws Exception {
        Page<Notice> noticePage = new PageImpl<>(Collections.singletonList(notice));
        when(noticeService.searchNoticesWithDate(any(), any(), any(), any(), any(Pageable.class))).thenReturn(noticePage);

        LocalDateTime now = LocalDateTime.now();
        String startDate = now.toString();
        String endDate = now.plusDays(7).toString();

        mockMvc.perform(get("/api/notices")
                        .param("searchType", "title")
                        .param("keyword", "테스트")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("테스트 공지사항"));
    }
}