package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminCollectionStatusResponse {

    private long matchCount;
    private long participantCount;
    private long championStatCount;
    private long seedPlayerCount;
    private long enabledSeedPlayerCount;
    private long rankingPlayerCount;

    private long totalSavedMatchesBySeeds;
    private long failedSeedCount;

    private String latestPatch;
    private long latestPatchMatchCount;
    private long latestPatchStatRows;
    private long latestPatchTotalGames;
    private int targetPatchGames;
    private double latestPatchProgressPercent;

    private LocalDateTime lastCollectedAt;
    private LocalDateTime lastMatchCreatedAt;
    private LocalDateTime lastStatsUpdatedAt;

    private boolean autoCollectEnabled;
    private int matchCountPerPlayer;
    private int maxPlayersPerCycle;
    private long fixedDelayMs;
    private long delayBetweenPlayersMs;

    private List<AdminSeedStatusResponse> recentSeeds;
}
