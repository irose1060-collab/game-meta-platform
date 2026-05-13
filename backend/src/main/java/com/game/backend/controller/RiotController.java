package com.game.backend.controller;

import com.game.backend.dto.RiotAccountResponse;
import com.game.backend.service.RiotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/riot")
@RequiredArgsConstructor
public class RiotController {
    private final RiotService riotService;

    @GetMapping("/account")
    public ResponseEntity<RiotAccountResponse> getAccountByRiotId(
            @RequestParam String gameName,
            @RequestParam String tagLine) {
        return ResponseEntity.ok(riotService.getAccountByRiotId(gameName, tagLine));
    }
}
