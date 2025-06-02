package pre.assignment.rsupport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import pre.assignment.rsupport.domain.Attachment;
import pre.assignment.rsupport.domain.Notice;
import pre.assignment.rsupport.repository.NoticeRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private NoticeService noticeService;

    private Notice notice;
    private List<MultipartFile> files;

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

        files = Collections.singletonList(
                new MockMultipartFile(
                        "file",
                        "test.pdf",
                        "application/pdf",
                        "test content".getBytes()
                )
        );
    }

    @Test
    void createNotice() throws IOException {
        when(noticeRepository.save(any(Notice.class))).thenReturn(notice);
        when(fileService.saveFiles(any(), any())).thenReturn(Collections.emptyList());

        Notice result = noticeService.createNotice(notice, files);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 공지사항");
        verify(noticeRepository).save(any(Notice.class));
        verify(fileService).saveFiles(any(), any());
    }

    @Test
    void updateNotice() throws IOException {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(noticeRepository.save(any(Notice.class))).thenReturn(notice);
        when(fileService.saveFiles(any(), any())).thenReturn(Collections.emptyList());

        Notice result = noticeService.updateNotice(1L, notice, files);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 공지사항");
        verify(noticeRepository, times(1)).findById(1L);
        verify(noticeRepository, times(1)).save(any(Notice.class));
        verify(fileService, times(1)).saveFiles(any(), any());
    }

    @Test
    void deleteNotice() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        doNothing().when(noticeRepository).delete(any(Notice.class));

        noticeService.deleteNotice(1L);

        verify(noticeRepository).findById(1L);
        verify(noticeRepository).delete(any(Notice.class));
    }

    @Test
    void getNotice() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(noticeRepository.save(any(Notice.class))).thenReturn(notice);

        Notice result = noticeService.getNotice(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 공지사항");
        assertThat(result.getViewCount()).isEqualTo(1);
        verify(noticeRepository).findById(1L);
        verify(noticeRepository).save(any(Notice.class));
    }

    @Test
    void searchNotices() {
        Page<Notice> noticePage = new PageImpl<>(Collections.singletonList(notice));
        when(noticeRepository.searchNotices(any(), any(), any(Pageable.class))).thenReturn(noticePage);

        Page<Notice> result = noticeService.searchNotices("title", "테스트", PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 공지사항");
        verify(noticeRepository).searchNotices(any(), any(), any(Pageable.class));
    }

    @Test
    void searchNoticesWithDate() {
        Page<Notice> noticePage = new PageImpl<>(Collections.singletonList(notice));
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        when(noticeRepository.searchNoticesWithDate(any(), any(), any(), any(), any(Pageable.class))).thenReturn(noticePage);

        Page<Notice> result = noticeService.searchNoticesWithDate("title", "테스트", startDate, endDate, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 공지사항");
        verify(noticeRepository).searchNoticesWithDate(any(), any(), any(), any(), any(Pageable.class));
    }
} 