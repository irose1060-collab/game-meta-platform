package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PatchNoteResponse {

    private String title;
    private String patchVersion;
    private String category;
    private String publishedAt;
    private String summary;
    private String officialUrl;
}
