package com.game.backend.controller;

import com.game.backend.dto.ChampionStatResponse;
import com.game.backend.dto.CollectionResultResponse;
import com.game.backend.dto.RiotAccountResponse;
import com.game.backend.dto.StatsRebuildResponse;
import com.game.backend.service.ChampionStatsService;
import com.game.backend.service.RiotDataCollectionService;
import com.game.backend.service.RiotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/riot")
@RequiredArgsConstructor
public class RiotController {
    private final RiotService riotService;
    private final RiotDataCollectionService riotDataCollectionService;
    private final ChampionStatsService championStatsService;

    @GetMapping("/account")
    public RiotAccountResponse getAccountByRiotId(
            @RequestParam String gameName,
            @RequestParam String tagLine
    ) {
        return riotService.getAccountByRiotId(gameName, tagLine);
    }

    /**
     * 예시:
     * POST /api/riot/collect?gameName=Hide%20on%20bush&tagLine=KR1&count=10
     */
    @PostMapping("/collect")
    public CollectionResultResponse collectRecentMatches(
            @RequestParam String gameName,
            @RequestParam String tagLine,
            @RequestParam(defaultValue = "10") int count
    ) {
        return riotDataCollectionService.collectRecentMatchesByRiotId(gameName, tagLine, count);
    }

    /**
     * 원천 경기 데이터(matches, match_participants)를 기준으로 champion_stats를 재계산한다.
     * 예시: POST /api/riot/stats/rebuild
     */
    @PostMapping("/stats/rebuild")
    public StatsRebuildResponse rebuildChampionStats(
            @RequestParam(required = false) String patch,
            @RequestParam(defaultValue = "420") int queueId
    ) {
        if (patch == null || patch.isBlank()) {
            return championStatsService.rebuildChampionStats();
        }
        return championStatsService.rebuildChampionStats(patch, queueId);
    }

    /**
     * 예시:
     * GET /api/riot/stats/champions?position=MIDDLE
     * GET /api/riot/stats/champions?position=JUNGLE&patch=15.10
     */
    @GetMapping("/stats/champions")
    public List<ChampionStatResponse> getChampionStats(
            @RequestParam(defaultValue = "MIDDLE") String position,
            @RequestParam(required = false) String patch
    ) {
        return championStatsService.getChampionStats(position, patch);
    }
}
