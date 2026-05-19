package com.game.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeRequest {
    private String title;
    private String content;
    private Boolean isPinned;
}
