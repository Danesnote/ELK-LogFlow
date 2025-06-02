package pre.assignment.rsupport.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pre.assignment.rsupport.domain.Notice;
import pre.assignment.rsupport.domain.Attachment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class NoticeDetailResponseDto {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private String author;
    private List<AttachmentDto> attachments;

    public NoticeDetailResponseDto(Notice notice) {
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.createdAt = notice.getCreatedAt();
        this.viewCount = notice.getViewCount();
        this.author = notice.getAuthor();
        this.attachments = notice.getAttachments().stream()
                .map(AttachmentDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AttachmentDto {
        private Long id;
        private String originalFileName;
        private String storedFileName;
        private String filePath;
        private Long fileSize;

        public AttachmentDto(Attachment attachment) {
            this.id = attachment.getId();
            this.originalFileName = attachment.getOriginalFilename();
            this.storedFileName = attachment.getStoredFilename();
            this.filePath = attachment.getFilePath();
            this.fileSize = attachment.getFileSize();
        }
    }
} 