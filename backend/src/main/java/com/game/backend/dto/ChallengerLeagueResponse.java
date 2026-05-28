package com.game.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChallengerLeagueResponse {

    private String leagueId;
    private String queue;
    private String tier;
    private String name;
    private List<ChallengerLeagueEntryResponse> entries;
}