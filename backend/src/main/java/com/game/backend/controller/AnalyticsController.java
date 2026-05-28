package com.game.backend.controller;

import com.game.backend.dto.AnalyticsOverviewResponse;
import com.game.backend.service.AnalyticsOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/riot/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsOverviewService analyticsOverviewService;

    @GetMapping("/overview")
    public AnalyticsOverviewResponse getOverview(
            @RequestParam(required = false) String patch,
            @RequestParam(defaultValue = "10") int minGames
    ) {
        return analyticsOverviewService.getOverview(patch, minGames);
    }
}
