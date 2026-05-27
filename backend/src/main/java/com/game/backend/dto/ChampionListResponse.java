package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChampionListResponse {
    private String id;
    private String key;
    private String nameKr;
    private String title;
    private String blurb;
    private String imageUrl;
    private List<String> tags;
    private String partype;
    private String position;
    private String tier;
    private Double winRate;
    private Double pickRate;
    private Double banRate;
    private Integer difficulty;
}
