package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PositionDistributionResponse {

    private String position;
    private long pickCount;
    private double percentage;
}
