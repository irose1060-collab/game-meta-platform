package com.game.backend.dto;

import java.util.List;

public record GeminiWinAnalysisRequest(
        String gameName,
        String tagLine,
        Integer totalMatches,
        Integer wins,
        Integer losses,
        List<MatchItem> matches
) {
    public record MatchItem(
            Boolean win,
            String championName,
            String position,
            Integer kills,
            Integer deaths,
            Integer assists,
            Double kda,
            Integer totalDamageDealtToChampions,
            Integer totalCs,
            Integer goldEarned,
            String queueType,
            String playedAtText,
            String gameDurationText
    ) {
    }
}