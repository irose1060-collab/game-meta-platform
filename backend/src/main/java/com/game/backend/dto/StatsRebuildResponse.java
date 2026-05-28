package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatsRebuildResponse {

    private int rebuiltCount;
    private String message;
}