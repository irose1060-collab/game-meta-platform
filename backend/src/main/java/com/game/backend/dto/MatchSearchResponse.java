package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchSearchResponse {

    private String gameName;
    private String tagLine;
    private String puuid;

    private Integer totalMatches;
    private Integer wins;
    private Integer losses;
    private Double winRate;
    private Double averageKda;

    private List<MatchSummaryResponse> matches;
}