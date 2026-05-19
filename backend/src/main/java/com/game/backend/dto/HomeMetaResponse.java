package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeMetaResponse {

    private HotChampion hotChampion;
    private PatchSummary patchSummary;
    private TeamCompSummary teamCompSummary;
    private AiFeedbackSummary aiFeedbackSummary;

    @Getter
    @Builder
    public static class HotChampion {
        private String name;
        private String nameKr;
        private String championKey;
        private String position;
        private String imageUrl;
        private Double winRate;
        private Double pickRate;
        private Double banRate;
        private String source;
    }

    @Getter
    @Builder
    public static class PatchSummary {
        private String version;
        private String summary;
        private String detail1;
        private String detail2;
        private String detail3;
        private String source;
    }

    @Getter
    @Builder
    public static class TeamCompSummary {
        private String apStatus;
        private Integer apRatio;
        private String ccStatus;
        private Integer ccScore;
        private Integer expectedWinRate;
        private String source;
    }

    @Getter
    @Builder
    public static class AiFeedbackSummary {
        private String feedback1;
        private String feedback2;
        private String feedback3;
        private String source;
    }
}
