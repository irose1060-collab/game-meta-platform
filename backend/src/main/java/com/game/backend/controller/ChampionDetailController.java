package com.game.backend.controller;

import com.game.backend.dto.ChampionAnalyticsDetailResponse;
import com.game.backend.dto.ChampionDetailResponse;
import com.game.backend.service.ChampionDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/riot/stats/champions")
@RequiredArgsConstructor
public class ChampionDetailController {

    private final ChampionDetailService championDetailService;

    @GetMapping("/detail")
    public ChampionAnalyticsDetailResponse getChampionDetail(
            @RequestParam int championId,
            @RequestParam String position,
            @RequestParam(required = false) String patch
    ) {
        return championDetailService.getChampionDetail(championId, position, patch);
    }
}