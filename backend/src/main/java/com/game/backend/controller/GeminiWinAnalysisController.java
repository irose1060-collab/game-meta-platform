package com.game.backend.controller;

import com.game.backend.dto.AiWinAnalysisResponse;
import com.game.backend.dto.GeminiWinAnalysisRequest;
import com.game.backend.service.GeminiWinAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class GeminiWinAnalysisController {

    private final GeminiWinAnalysisService geminiWinAnalysisService;

    @PostMapping("/gemini-win-analysis")
    public AiWinAnalysisResponse analyze(@RequestBody GeminiWinAnalysisRequest request) {
        return geminiWinAnalysisService.analyze(request);
    }
}