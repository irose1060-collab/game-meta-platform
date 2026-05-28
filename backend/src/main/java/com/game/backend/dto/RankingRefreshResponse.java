package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RankingRefreshResponse {

    private int requestedLimit;
    private int savedCount;
    private int seedAddedCount;
    private String message;
    private List<RankingPlayerResponse> players;
}