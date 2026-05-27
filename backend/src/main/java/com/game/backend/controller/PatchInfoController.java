package com.game.backend.controller;

import com.game.backend.dto.PatchInfoResponse;
import com.game.backend.service.PatchInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patches")
@RequiredArgsConstructor
public class PatchInfoController {

    private final PatchInfoService patchInfoService;

    @GetMapping("/info")
    public PatchInfoResponse getPatchInfo() {
        return patchInfoService.getPatchInfo();
    }
}
