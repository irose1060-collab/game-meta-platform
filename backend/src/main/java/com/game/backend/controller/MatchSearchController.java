package com.game.backend.controller;

import com.game.backend.dto.MatchSearchResponse;
import com.game.backend.service.MatchSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchSearchController {

    private final MatchSearchService matchSearchService;

    @GetMapping("/search")
    public MatchSearchResponse searchMatches(
            @RequestParam String gameName,
            @RequestParam String tagLine,
            @RequestParam(defaultValue = "20") int count
    ) {
        return matchSearchService.searchMatches(gameName, tagLine, count);
    }
}