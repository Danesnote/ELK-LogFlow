package pre.assignment.rsupport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import pre.assignment.rsupport.domain.Attachment;
import pre.assignment.rsupport.domain.Notice;
import pre.assignment.rsupport.repository.AttachmentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private List<MultipartFile> files;

    @BeforeEach
    void setUp() {
        notice = Notice.builder()
                .id(1L)
                .title("테스트 공지사항")
                .content("테스트 내용")
                .author("테스트 작성자")
                .build();

        files = List.of(
                new MockMultipartFile(
                        "file",
                        "test.pdf",
                        "application/pdf",
                        "test content".getBytes()
                )
        );

        // uploadDir 설정
        ReflectionTestUtils.setField(fileService, "uploadDir", "./test-uploads");
    }

    @Test
    void saveFiles() throws IOException {
        // given
        Path uploadPath = Path.of("./test-uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // when
        List<Attachment> attachments = fileService.saveFiles(files, notice);

        // then
        assertThat(attachments).isNotNull();
        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0).getOriginalFilename()).isEqualTo("test.pdf");
        assertThat(attachments.get(0).getNotice()).isEqualTo(notice);

        // cleanup
        Files.walk(uploadPath)
                .map(Path::toFile)
                .forEach(file -> file.delete());
        Files.deleteIfExists(uploadPath);
    }

    @Test
    void deleteFiles() throws IOException {
        // given
        Path uploadPath = Path.of("./test-uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<Attachment> attachments = fileService.saveFiles(files, notice);

        // when
        fileService.deleteFiles(attachments);

        // then
        assertThat(Files.exists(uploadPath)).isFalse();
    }
}