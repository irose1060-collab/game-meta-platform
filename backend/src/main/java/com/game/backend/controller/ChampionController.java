package com.game.backend.controller;

import com.game.backend.dto.ChampionDetailResponse;
import com.game.backend.dto.ChampionListResponse;
import com.game.backend.service.ChampionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/champions")
@RequiredArgsConstructor
public class ChampionController {

    private final ChampionService championService;

    @GetMapping
    public List<ChampionListResponse> getChampions() {
        return championService.getChampionList();
    }

    @GetMapping("/{championId}")
    public ChampionDetailResponse getChampionDetail(@PathVariable String championId) {
        return championService.getChampionDetail(championId);
    }
}
