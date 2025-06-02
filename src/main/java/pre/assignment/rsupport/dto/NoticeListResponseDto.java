package pre.assignment.rsupport.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pre.assignment.rsupport.domain.Notice;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NoticeListResponseDto {
    private Long id;
    private String title;
    private boolean hasAttachment;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private String author;

    public NoticeListResponseDto(Notice notice) {
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.hasAttachment = !notice.getAttachments().isEmpty();
        this.createdAt = notice.getCreatedAt();
        this.viewCount = notice.getViewCount();
        this.author = notice.getAuthor();
    }
} 