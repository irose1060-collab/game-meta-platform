package com.game.backend.controller;

import com.game.backend.dto.AdminCollectionStatusResponse;
import com.game.backend.dto.AutoCollectionRunResponse;
import com.game.backend.dto.StatsRebuildResponse;
import com.game.backend.service.AdminCollectionStatusService;
import com.game.backend.service.AutoDataCollectionService;
import com.game.backend.service.ChampionStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/collection")
@RequiredArgsConstructor
public class AdminCollectionStatusController {

    private final AdminCollectionStatusService adminCollectionStatusService;
    private final AutoDataCollectionService autoDataCollectionService;
    private final ChampionStatsService championStatsService;

    @GetMapping("/status")
    public AdminCollectionStatusResponse getCollectionStatus() {
        return adminCollectionStatusService.getStatus();
    }

    /** 관리자 페이지에서 최신 패치 수집을 즉시 1회 실행한다. */
    @PostMapping("/run")
    public AutoCollectionRunResponse runCollectionNow() {
        return autoDataCollectionService.runManualCollectionCycle();
    }

    /** 특정 패치 또는 최신 패치의 champion_stats만 재집계한다. */
    @PostMapping("/stats/rebuild")
    public StatsRebuildResponse rebuildStats(
            @RequestParam(required = false) String patch,
            @RequestParam(defaultValue = "420") int queueId
    ) {
        if (patch == null || patch.isBlank()) {
            return championStatsService.rebuildChampionStats();
        }

        return championStatsService.rebuildChampionStats(patch, queueId);
    }
}
