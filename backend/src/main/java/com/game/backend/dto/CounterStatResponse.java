package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CounterStatResponse {

    private int enemyChampionId;
    private String enemyChampionName;
    private int games;
    private int wins;
    private double winRate;
}