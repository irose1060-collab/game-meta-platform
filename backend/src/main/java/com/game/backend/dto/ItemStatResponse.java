package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ItemStatResponse {

    private int itemId;
    private int games;
    private int wins;
    private double winRate;
}