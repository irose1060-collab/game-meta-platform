package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchTeamSummaryResponse {

    private Integer teamId;
    private Boolean win;

    private Integer totalKills;
    private Integer totalGold;

    private Integer baronKills;
    private Integer dragonKills;
    private Integer riftHeraldKills;
    private Integer hordeKills;
    private Integer towerKills;
    private Integer inhibitorKills;

    private List<AssetDto> bans;
}