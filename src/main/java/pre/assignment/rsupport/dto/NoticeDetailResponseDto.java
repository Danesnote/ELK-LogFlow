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
public class NoticeDetailResponseDto {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private String author;
    private List<AttachmentDto> attachments;

    @Getter
    @Builder
    public static class AttachmentDto {
        private Long id;
        private String originalFilename;
        private String storedFilename;
        private String filePath;
        private Long fileSize;
    }

    public static NoticeDetailResponseDto from(Notice notice) {
        return NoticeDetailResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .createdAt(notice.getCreatedAt())
                .viewCount(notice.getViewCount())
                .author(notice.getAuthor())
                .attachments(notice.getAttachments().stream()
                        .map(attachment -> AttachmentDto.builder()
                                .id(attachment.getId())
                                .originalFilename(attachment.getOriginalFilename())
                                .storedFilename(attachment.getStoredFilename())
                                .filePath(attachment.getFilePath())
                                .fileSize(attachment.getFileSize())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
} 