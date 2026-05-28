package com.game.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChallengerLeagueEntryResponse {

    private String puuid;

    // Riot 응답 버전에 따라 없을 수도 있음. 있으면 그대로 사용.
    private String gameName;
    private String tagLine;

    // 과거/일부 응답 호환용. 현재 로직에서는 puuid 우선 사용.
    private String summonerId;

    private int leaguePoints;
    private int wins;
    private int losses;
    private boolean veteran;
    private boolean inactive;
    private boolean freshBlood;
    private boolean hotStreak;
}