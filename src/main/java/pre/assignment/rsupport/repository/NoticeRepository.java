package pre.assignment.rsupport.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pre.assignment.rsupport.domain.Notice;
import java.time.LocalDateTime;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    
    @Query("SELECT n FROM Notice n WHERE " +
           "(:searchType = 'TITLE_CONTENT' AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%)) OR " +
           "(:searchType = 'TITLE' AND n.title LIKE %:keyword%)")
    Page<Notice> searchNotices(
        @Param("searchType") String searchType,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("SELECT n FROM Notice n WHERE " +
           "(:searchType = 'TITLE_CONTENT' AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%)) OR " +
           "(:searchType = 'TITLE' AND n.title LIKE %:keyword%) AND " +
           "n.createdAt BETWEEN :startDate AND :endDate")
    Page<Notice> searchNoticesWithDate(
        @Param("searchType") String searchType,
        @Param("keyword") String keyword,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
} 