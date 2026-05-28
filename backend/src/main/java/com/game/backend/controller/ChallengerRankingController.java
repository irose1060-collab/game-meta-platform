package com.game.backend.controller;

import com.game.backend.dto.RankingCollectResponse;
import com.game.backend.dto.RankingPlayerResponse;
import com.game.backend.dto.RankingRefreshResponse;
import com.game.backend.service.ChallengerRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/riot/ranking/challenger")
@RequiredArgsConstructor
public class ChallengerRankingController {

    private final ChallengerRankingService challengerRankingService;

    @PostMapping("/refresh")
    public RankingRefreshResponse refreshRanking(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return challengerRankingService.refreshKrChallengerRanking(limit);
    }

    @GetMapping("/top")
    public List<RankingPlayerResponse> getTopRanking(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return challengerRankingService.getTopRankingPlayers(limit);
    }

    @PostMapping("/collect")
    public RankingCollectResponse collectFromRankingPlayers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "5") int count
    ) {
        return challengerRankingService.collectMatchesFromRankingPlayers(limit, count);
    }
}