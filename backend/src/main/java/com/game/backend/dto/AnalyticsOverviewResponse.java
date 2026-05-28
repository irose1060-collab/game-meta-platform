package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalyticsOverviewResponse {

    private AnalyticsSummaryResponse summary;

    private List<PositionDistributionResponse> positionDistribution;

    private List<PositionTopChampionsResponse> positionTopChampions;

    private List<AnalyticsChampionResponse> topWinRateChampions;
    private List<AnalyticsChampionResponse> topPickRateChampions;
    private List<AnalyticsChampionResponse> topDamageChampions;
    private List<AnalyticsChampionResponse> topKdaChampions;

    private List<AnalyticsChampionResponse> scatterChampions;
}
