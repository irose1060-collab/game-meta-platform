package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PatchInfoResponse {
    private String latestVersion;
    private String previousVersion;
    private String source;
    private Integer championCount;
    private Integer previousChampionCount;
    private List<String> recentVersions;
    private List<PatchChampionInfo> newChampions;
    private List<PatchChampionInfo> removedChampions;
    private List<PatchChampionInfo> updatedChampions;
    private List<String> summaryLines;

    @Getter
    @Builder
    public static class PatchChampionInfo {
        private String id;
        private String key;
        private String nameKr;
        private String title;
        private String imageUrl;
        private String changeType;
        private String description;
    }
}
