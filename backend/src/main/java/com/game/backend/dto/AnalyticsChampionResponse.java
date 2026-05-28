package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyticsChampionResponse {

    private String patch;
    private int queueId;
    private String position;

    private int championId;
    private String championName;

    private int games;
    private int wins;

    private double winRate;
    private double pickRate;
    private double avgKda;
    private double avgDamage;
    private double avgGold;
    private double avgCs;
    private double avgVisionScore;

    private double tierScore;
    private String tier;
}
