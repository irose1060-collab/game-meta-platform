package com.game.backend.controller;

import com.game.backend.dto.NoticeResponse;
import com.game.backend.entity.Notice;
import com.game.backend.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeRepository noticeRepository;

    @GetMapping("/recent")
    public List<NoticeResponse> getRecentNotices() {
        return noticeRepository.findTop3ByStatusOrderByIsPinnedDescCreatedAtDesc("VISIBLE")
                .stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @GetMapping
    public List<NoticeResponse> getNotices() {
        return noticeRepository.findByStatusOrderByIsPinnedDescCreatedAtDesc("VISIBLE")
                .stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public NoticeResponse getNotice(@PathVariable Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        notice.setViewCount(notice.getViewCount() + 1);
        noticeRepository.save(notice);

        return NoticeResponse.from(notice);
    }
}