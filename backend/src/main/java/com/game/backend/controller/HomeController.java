package com.game.backend.controller;

import com.game.backend.dto.HomeMetaResponse;
import com.game.backend.service.HomeMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeMetaService homeMetaService;

    @GetMapping("/meta")
    public HomeMetaResponse getHomeMeta() {
        return homeMetaService.getHomeMeta();
    }
}
