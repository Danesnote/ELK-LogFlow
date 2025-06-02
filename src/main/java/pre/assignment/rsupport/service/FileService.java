package pre.assignment.rsupport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pre.assignment.rsupport.domain.Attachment;
import pre.assignment.rsupport.domain.Notice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    public List<Attachment> saveFiles(List<MultipartFile> files, Notice notice) throws IOException {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        // 업로드 디렉토리가 없으면 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<Attachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFileName = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID().toString() + getFileExtension(originalFileName);
            String filePath = uploadPath.resolve(storedFileName).toString();

            // 파일 저장
            Files.copy(file.getInputStream(), Paths.get(filePath));

            // Attachment 엔티티 생성
            Attachment attachment = new Attachment();
            attachment.setOriginalFilename(originalFileName);
            attachment.setStoredFilename(storedFileName);
            attachment.setFilePath(filePath);
            attachment.setFileSize(file.getSize());
            attachment.setNotice(notice);

            attachments.add(attachment);
        }

        return attachments;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex);
    }

    public void deleteFiles(List<Attachment> attachments) {
        if (attachments == null) return;

        for (Attachment attachment : attachments) {
            try {
                Files.deleteIfExists(Paths.get(attachment.getFilePath()));
            } catch (IOException e) {
                // 로그 기록
            }
        }
    }
} 