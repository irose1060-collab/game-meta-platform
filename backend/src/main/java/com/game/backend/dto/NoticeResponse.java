package com.game.backend.dto;

import com.game.backend.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeResponse {
    private Long id;
    private String title;
    private String content;
    private Boolean isPinned;
    private String status;
    private Integer viewCount;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .isPinned(notice.getIsPinned())
                .status(notice.getStatus())
                .viewCount(notice.getViewCount())
                .createdBy(notice.getCreatedBy())
                .updatedBy(notice.getUpdatedBy())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
