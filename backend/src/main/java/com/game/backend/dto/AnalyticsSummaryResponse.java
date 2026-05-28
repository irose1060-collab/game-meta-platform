package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyticsSummaryResponse {

    private long matchCount;
    private long participantCount;
    private long championStatCount;
    private long seedPlayerCount;
    private long rankingPlayerCount;

    private String latestPatch;
    private long analyzedPickCount;
    private int minGames;
}
