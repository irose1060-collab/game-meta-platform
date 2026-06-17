package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiWinAnalysisResponse {

    private String gameName;
    private String tagLine;

    private int totalMatches;
    private int wins;
    private int losses;

    private double winRate;
    private double averageKda;
    private double averageKills;
    private double averageDeaths;
    private double averageAssists;
    private double averageDamage;
    private double averageCs;
    private double averageGold;

    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
}