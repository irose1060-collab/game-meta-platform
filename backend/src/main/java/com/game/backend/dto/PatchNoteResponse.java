package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PatchNoteResponse {
    private String title;
    private String date;
    private String description;
    private String url;
    private String slug;
}