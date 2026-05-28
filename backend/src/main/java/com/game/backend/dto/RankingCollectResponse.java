package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingCollectResponse {

    private int requestedPlayerLimit;
    private int matchCountPerPlayer;
    private int processedPlayerCount;
    private int totalSavedMatchCount;
    private int totalSkippedExistingMatchCount;
    private int totalFailedMatchCount;
    private int totalSavedParticipantCount;
    private String message;
}