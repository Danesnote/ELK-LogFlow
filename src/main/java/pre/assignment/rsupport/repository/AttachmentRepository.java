package pre.assignment.rsupport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pre.assignment.rsupport.domain.Attachment;
import pre.assignment.rsupport.domain.Notice;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByNotice(Notice notice);
} 