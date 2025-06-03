package pre.assignment.rsupport.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pre.assignment.rsupport.domain.Notice;
import pre.assignment.rsupport.dto.NoticeRequestDto;
import pre.assignment.rsupport.dto.NoticeResponseDto;
import pre.assignment.rsupport.dto.NoticeListResponseDto;
import pre.assignment.rsupport.dto.NoticeDetailResponseDto;
import pre.assignment.rsupport.service.NoticeService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "공지사항", description = "공지사항 관리 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 생성", description = "새로운 공지사항을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공지사항 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoticeResponseDto> createNotice(
            @Parameter(description = "공지사항 정보") @RequestPart("notice") NoticeRequestDto noticeDto,
            @Parameter(description = "첨부 파일") @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        Notice notice = noticeDto.toEntity();
        Notice savedNotice = noticeService.createNotice(notice, files);
        return ResponseEntity.ok(NoticeResponseDto.from(savedNotice));
    }

    @Operation(summary = "공지사항 수정", description = "기존 공지사항을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공지사항 수정 성공"),
            @ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoticeResponseDto> updateNotice(
            @Parameter(description = "공지사항 ID") @PathVariable Long id,
            @Parameter(description = "공지사항 정보") @RequestPart("notice") NoticeRequestDto noticeDto,
            @Parameter(description = "첨부 파일") @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        Notice notice = noticeDto.toEntity();
        Notice updatedNotice = noticeService.updateNotice(id, notice, files);
        return ResponseEntity.ok(NoticeResponseDto.from(updatedNotice));
    }

    @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "공지사항 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(
            @Parameter(description = "공지사항 ID") @PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public ResponseEntity<Page<NoticeListResponseDto>> getNoticeList(Pageable pageable) {
        Page<Notice> notices = noticeService.getNoticeList(pageable);
        return ResponseEntity.ok(notices.map(NoticeListResponseDto::from));
    }

    @Operation(summary = "공지사항 상세 조회", description = "ID로 공지사항을 상세 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = NoticeDetailResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<NoticeDetailResponseDto> getNoticeDetail(
            @Parameter(description = "공지사항 ID") @PathVariable Long id) {
        Notice notice = noticeService.getNotice(id);
        return ResponseEntity.ok(NoticeDetailResponseDto.from(notice));
    }

    @Operation(summary = "공지사항 검색", description = "제목과 내용으로 공지사항을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<Page<NoticeListResponseDto>> searchNotices(
            @Parameter(description = "검색 필드 (title, content)") @RequestParam String field,
            @Parameter(description = "검색어") @RequestParam String keyword,
            @Parameter(description = "페이지 정보") Pageable pageable) {
        Page<Notice> notices = noticeService.searchNotices(field, keyword, pageable);
        return ResponseEntity.ok(notices.map(NoticeListResponseDto::from));
    }

    @Operation(summary = "기간별 공지사항 검색", description = "제목, 내용, 기간으로 공지사항을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/search/date")
    public ResponseEntity<Page<NoticeListResponseDto>> searchNoticesWithDate(
            @Parameter(description = "검색 필드 (title, content)") @RequestParam String field,
            @Parameter(description = "검색어") @RequestParam String keyword,
            @Parameter(description = "시작일 (yyyy-MM-dd HH:mm:ss)") @RequestParam String startDate,
            @Parameter(description = "종료일 (yyyy-MM-dd HH:mm:ss)") @RequestParam String endDate,
            @Parameter(description = "페이지 정보") Pageable pageable) {
        LocalDateTime startDateTime = LocalDateTime.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endDateTime = LocalDateTime.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Page<Notice> notices = noticeService.searchNoticesWithDate(field, keyword, startDateTime, endDateTime, pageable);
        return ResponseEntity.ok(notices.map(NoticeListResponseDto::from));
    }
}