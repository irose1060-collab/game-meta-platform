package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpellStatResponse {

    private int spell1Id;
    private int spell2Id;
    private int games;
    private int wins;
    private double winRate;
}