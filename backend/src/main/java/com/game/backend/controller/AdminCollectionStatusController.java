package com.game.backend.controller;

import com.game.backend.dto.AdminCollectionStatusResponse;
import com.game.backend.service.AdminCollectionStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/collection")
@RequiredArgsConstructor
public class AdminCollectionStatusController {

    private final AdminCollectionStatusService adminCollectionStatusService;

    @GetMapping("/status")
    public AdminCollectionStatusResponse getCollectionStatus() {
        return adminCollectionStatusService.getStatus();
    }
}