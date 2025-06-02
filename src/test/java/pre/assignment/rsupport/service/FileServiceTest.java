package pre.assignment.rsupport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import pre.assignment.rsupport.domain.Attachment;
import pre.assignment.rsupport.domain.Notice;
import pre.assignment.rsupport.repository.AttachmentRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @InjectMocks
    private FileService fileService;

    @InjectMocks
    private NoticeService noticeService;

    private Notice notice;
    private MultipartFile file;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        attachment = Attachment.builder()
                .id(1L)
                .originalFilename("test.pdf")
                .storedFilename("stored_test.pdf")
                .filePath("/uploads/stored_test.pdf")
                .fileSize(1024L)
                .build();

        notice = Notice.builder()
                .id(1L)
                .title("테스트 공지사항")
                .content("테스트 내용")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(7))
                .author("테스트 작성자")
                .viewCount(0)
                .build();
    }

    @Test
    void saveFiles() throws IOException {
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);

        List<Attachment> result = fileService.saveFiles(Collections.singletonList(file), notice);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalFilename()).isEqualTo("test.pdf");
        verify(attachmentRepository, times(1)).save(any(Attachment.class));
    }

    @Test
    void deleteFiles() {
        doNothing().when(attachmentRepository).delete(any(Attachment.class));

        fileService.deleteFiles(Collections.singletonList(attachment));

        verify(attachmentRepository, times(1)).delete(any(Attachment.class));
    }

}