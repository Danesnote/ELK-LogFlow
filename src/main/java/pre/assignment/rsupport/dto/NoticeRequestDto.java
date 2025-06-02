package pre.assignment.rsupport.dto;

import lombok.*;
import pre.assignment.rsupport.domain.Notice;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeRequestDto {
    private String title;
    private String content;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String author;

    public Notice toEntity() {
        return Notice.builder()
                .title(this.title)
                .content(this.content)
                .startDateTime(this.startDateTime)
                .endDateTime(this.endDateTime)
                .author(this.author)
                .viewCount(0)
                .build();
    }
} 