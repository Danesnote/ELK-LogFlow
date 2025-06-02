package pre.assignment.rsupport.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import pre.assignment.rsupport.domain.Notice;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    private Notice notice;

    @BeforeEach
    void setUp() {
        notice = Notice.builder()
                .title("테스트 공지사항")
                .content("테스트 내용")
                .startDateTime(LocalDateTime.now())
                .endDateTime(LocalDateTime.now().plusDays(7))
                .author("테스트 작성자")
                .viewCount(0)
                .build();
    }

    @Test
    void save() {
        Notice savedNotice = noticeRepository.save(notice);

        assertThat(savedNotice).isNotNull();
        assertThat(savedNotice.getId()).isNotNull();
        assertThat(savedNotice.getTitle()).isEqualTo("테스트 공지사항");
    }

    @Test
    void findById() {
        Notice savedNotice = noticeRepository.save(notice);

        Notice foundNotice = noticeRepository.findById(savedNotice.getId()).orElse(null);

        assertThat(foundNotice).isNotNull();
        assertThat(foundNotice.getId()).isEqualTo(savedNotice.getId());
        assertThat(foundNotice.getTitle()).isEqualTo("테스트 공지사항");
    }

    @Test
    void delete() {
        Notice savedNotice = noticeRepository.save(notice);

        noticeRepository.delete(savedNotice);

        assertThat(noticeRepository.findById(savedNotice.getId())).isEmpty();
    }
} 