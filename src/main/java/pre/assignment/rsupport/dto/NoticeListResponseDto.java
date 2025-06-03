package pre.assignment.rsupport.dto;

import lombok.Builder;
import lombok.Getter;
import pre.assignment.rsupport.domain.Notice;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeListResponseDto {
    private Long id;
    private String title;
    private boolean hasAttachment;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private String author;

    public static NoticeListResponseDto from(Notice notice) {
        return NoticeListResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .hasAttachment(!notice.getAttachments().isEmpty())
                .createdAt(notice.getCreatedAt())
                .viewCount(notice.getViewCount())
                .author(notice.getAuthor())
                .build();
    }
} 