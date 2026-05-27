package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchParticipantResponse {

    private String puuid;
    private String summonerId;
    private String summonerName;

    private String riotGameName;
    private String riotTagLine;

    private Integer teamId;
    private String teamPosition;

    private String championName;
    private String championImageUrl;
    private Integer championLevel;

    private Boolean win;

    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Double kda;

    private Integer killParticipation;
    private Double damageShare;

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
}