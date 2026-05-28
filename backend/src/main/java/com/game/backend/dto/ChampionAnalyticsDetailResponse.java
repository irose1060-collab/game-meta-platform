package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChampionAnalyticsDetailResponse {

    private ChampionStatResponse basic;

    private List<SpellStatResponse> spells;

    private List<ItemStatResponse> items;

    private List<CounterStatResponse> hardCounters;

    private List<CounterStatResponse> easyMatchups;
}