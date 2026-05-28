package com.game.backend.dto;

import com.game.backend.entity.ChallengerRankingPlayer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingPlayerResponse {

    private int rankNo;
    private String gameName;
    private String tagLine;
    private String riotId;
    private String puuid;
    private int leaguePoints;
    private int wins;
    private int losses;
    private double winRate;
    private boolean seedAdded;

    public static RankingPlayerResponse from(ChallengerRankingPlayer player) {
        String riotId = player.getGameName() == null
                ? null
                : player.getGameName() + "#" + player.getTagLine();

        return RankingPlayerResponse.builder()
                .rankNo(player.getRankNo())
                .gameName(player.getGameName())
                .tagLine(player.getTagLine())
                .riotId(riotId)
                .puuid(player.getPuuid())
                .leaguePoints(player.getLeaguePoints())
                .wins(player.getWins())
                .losses(player.getLosses())
                .winRate(player.getWinRate())
                .seedAdded(player.isSeedAdded())
                .build();
    }
}