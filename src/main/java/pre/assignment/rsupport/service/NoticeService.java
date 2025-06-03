package pre.assignment.rsupport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pre.assignment.rsupport.domain.Attachment;
import pre.assignment.rsupport.domain.Notice;
import pre.assignment.rsupport.repository.NoticeRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final FileService fileService;

    @Transactional
    public Notice createNotice(Notice notice, List<MultipartFile> files) throws IOException {
        Notice savedNotice = noticeRepository.save(notice);
        if (files != null && !files.isEmpty()) {
            List<Attachment> attachments = fileService.saveFiles(files, savedNotice);
            savedNotice.getAttachments().addAll(attachments);
        }
        return savedNotice;
    }

    @Transactional
    @CacheEvict(value = "notice", key = "#id")
    public Notice updateNotice(Long id, Notice notice, List<MultipartFile> files) throws IOException {
        Notice existingNotice = noticeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notice not found"));
        
        // 기존 파일 삭제
        fileService.deleteFiles(existingNotice.getAttachments());
        existingNotice.getAttachments().clear();
        
        // 공지사항 정보 업데이트
        existingNotice.setTitle(notice.getTitle());
        existingNotice.setContent(notice.getContent());
        existingNotice.setStartDateTime(notice.getStartDateTime());
        existingNotice.setEndDateTime(notice.getEndDateTime());
        
        // 새 파일 저장
        if (files != null && !files.isEmpty()) {
            List<Attachment> attachments = fileService.saveFiles(files, existingNotice);
            existingNotice.getAttachments().addAll(attachments);
        }
        
        return existingNotice;
    }

    @Transactional
    @CacheEvict(value = "notice", key = "#id")
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notice not found"));
        
        // 첨부파일 삭제
        fileService.deleteFiles(notice.getAttachments());
        
        noticeRepository.delete(notice);
    }

    @Transactional
    @Cacheable(value = "notice", key = "#id")
    public Notice getNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notice not found"));
        notice.setViewCount(notice.getViewCount() + 1);
        return noticeRepository.save(notice);
    }

    public Page<Notice> searchNotices(String searchType, String keyword, Pageable pageable) {
        return noticeRepository.searchNotices(searchType, keyword, pageable);
    }

    public Page<Notice> searchNoticesWithDate(
        String searchType, 
        String keyword, 
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        Pageable pageable
    ) {
        return noticeRepository.searchNoticesWithDate(searchType, keyword, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notice> getNoticeList(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }
} 