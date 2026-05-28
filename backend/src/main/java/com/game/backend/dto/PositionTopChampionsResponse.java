package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PositionTopChampionsResponse {

    private String position;
    private List<AnalyticsChampionResponse> champions;
}
