package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminSeedStatusResponse {

    private Long id;
    private String gameName;
    private String tagLine;
    private boolean enabled;
    private LocalDateTime lastCollectedAt;
    private int totalSavedMatches;
    private int totalFailedCount;
    private String lastResultMessage;
}