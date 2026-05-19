package com.game.backend.controller;

import com.game.backend.dto.NoticeRequest;
import com.game.backend.dto.NoticeResponse;
import com.game.backend.entity.Notice;
import com.game.backend.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeRepository noticeRepository;

    @GetMapping
    public List<NoticeResponse> getAdminNotices() {
        return noticeRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Notice::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(NoticeResponse::from)
                .toList();
    }

    @PostMapping
    public NoticeResponse createNotice(@RequestBody NoticeRequest request) {
        validateRequest(request);

        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .isPinned(request.getIsPinned() != null && request.getIsPinned())
                .status("VISIBLE")
                .viewCount(0)
                .build();

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    @PutMapping("/{id}")
    public NoticeResponse updateNotice(@PathVariable Long id, @RequestBody NoticeRequest request) {
        validateRequest(request);

        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setIsPinned(request.getIsPinned() != null && request.getIsPinned());

        return NoticeResponse.from(noticeRepository.save(notice));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteNotice(@PathVariable Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        noticeRepository.delete(notice);
        return Map.of("message", "공지사항이 삭제되었습니다.");
    }

    private void validateRequest(NoticeRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("공지 제목을 입력해주세요.");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new RuntimeException("공지 내용을 입력해주세요.");
        }
    }
}
