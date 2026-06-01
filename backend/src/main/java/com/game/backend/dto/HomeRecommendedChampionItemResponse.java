package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HomeRecommendedChampionItemResponse {

    private String patch;
    private String position;

    private int championId;
    private String championName;

    private int games;
    private int wins;

    private double winRate;
    private double pickRate;
    private double avgKda;
    private double avgDamage;

    private double tierScore;
    private String tier;

    private String reason;
}