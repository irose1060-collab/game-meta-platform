package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchSummaryResponse {

    private String matchId;

    private Long gameStartTimestamp;
    private String playedAtText;

    private String championName;
    private String championImageUrl;

    private Boolean win;
    private String resultText;

    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Double kda;

    private Integer killParticipation;

    private String position;
    private String gameMode;
    private String queueType;
    private Integer queueId;

    private Integer gameDurationSeconds;
    private String gameDurationText;

    private Integer totalCs;
    private Double csPerMinute;

    private Integer goldEarned;
    private Integer totalDamageDealtToChampions;
    private Integer totalDamageTaken;
    private Integer visionScore;

    private Integer wardsPlaced;
    private Integer wardsKilled;
    private Integer controlWardsPlaced;

    private Double opScore;
    private Integer opScoreRank;
    private String opScoreBadge;

    private String rankTier;

    private List<AssetDto> items;
    private List<AssetDto> summonerSpells;
    private List<AssetDto> runes;

    private List<ItemBuildStepResponse> itemBuild;
    private List<SkillOrderStepResponse> skillOrder;
    private String skillOrderText;

    private List<MatchParticipantResponse> blueTeam;
    private List<MatchParticipantResponse> redTeam;

    private Integer blueTeamTotalKills;
    private Integer redTeamTotalKills;
    private Integer blueTeamTotalGold;
    private Integer redTeamTotalGold;

    private Integer maxDamage;

    private MatchTeamSummaryResponse blueTeamSummary;
    private MatchTeamSummaryResponse redTeamSummary;
}