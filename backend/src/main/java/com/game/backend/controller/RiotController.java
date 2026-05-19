package com.game.backend.controller;

import com.game.backend.dto.RiotAccountResponse;
import com.game.backend.service.RiotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/riot")
@RequiredArgsConstructor
public class RiotController {
    private final RiotService riotService;

    @GetMapping("/account")
    public RiotAccountResponse getAccountByRiotId(
            @RequestParam String gameName,
            @RequestParam String tagLine
    ) {
        return riotService.getAccountByRiotId(gameName, tagLine);
    }
}
