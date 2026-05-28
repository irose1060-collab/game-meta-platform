package com.game.backend.controller;

import com.game.backend.dto.SeedPlayerRequest;
import com.game.backend.entity.SeedPlayer;
import com.game.backend.service.AutoDataCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/riot/auto-collect")
@RequiredArgsConstructor
public class AutoDataCollectionController {

    private final AutoDataCollectionService autoDataCollectionService;

    @GetMapping("/seeds")
    public List<SeedPlayer> getSeeds() {
        return autoDataCollectionService.getSeedPlayers();
    }

    @PostMapping("/seeds")
    public SeedPlayer addSeed(@RequestBody SeedPlayerRequest request) {
        return autoDataCollectionService.addSeedPlayer(
                request.getGameName(),
                request.getTagLine()
        );
    }

    @PatchMapping("/seeds/{id}/enable")
    public SeedPlayer enableSeed(@PathVariable Long id) {
        return autoDataCollectionService.toggleSeedPlayer(id, true);
    }

    @PatchMapping("/seeds/{id}/disable")
    public SeedPlayer disableSeed(@PathVariable Long id) {
        return autoDataCollectionService.toggleSeedPlayer(id, false);
    }
}