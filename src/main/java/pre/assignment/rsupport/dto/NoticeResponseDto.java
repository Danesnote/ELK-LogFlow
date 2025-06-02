package pre.assignment.rsupport.dto;

import lombok.Builder;
import lombok.Getter;
import pre.assignment.rsupport.domain.Attachment;
import pre.assignment.rsupport.domain.Notice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class NoticeResponseDto {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String author;
    private Integer viewCount;
    private List<AttachmentDto> attachments;

    @Getter
    @Builder
    public static class AttachmentDto {
        private Long id;
        private String originalFilename;
        private String storedFilename;
        private String filePath;
        private Long fileSize;
        private String contentType;
    }

    public static NoticeResponseDto from(Notice notice) {
        return NoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .startDateTime(notice.getStartDateTime())
                .endDateTime(notice.getEndDateTime())
                .author(notice.getAuthor())
                .viewCount(notice.getViewCount())
                .attachments(notice.getAttachments().stream()
                        .map(attachment -> AttachmentDto.builder()
                                .id(attachment.getId())
                                .originalFilename(attachment.getOriginalFilename())
                                .storedFilename(attachment.getStoredFilename())
                                .filePath(attachment.getFilePath())
                                .fileSize(attachment.getFileSize())
                                .contentType(attachment.getContentType())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
} 