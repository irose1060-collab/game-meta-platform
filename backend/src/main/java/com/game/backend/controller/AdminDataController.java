package com.game.backend.controller;

import com.game.backend.dto.AutoCollectionRunResponse;
import com.game.backend.entity.DataCollectionLog;
import com.game.backend.repository.DataCollectionLogRepository;
import com.game.backend.service.AutoDataCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/data")
@RequiredArgsConstructor
public class AdminDataController {

    private final DataCollectionLogRepository dataCollectionLogRepository;
    private final AutoDataCollectionService autoDataCollectionService;

    @GetMapping("/logs")
    public List<DataCollectionLog> getLogs() {
        return dataCollectionLogRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /** 기존 관리자 페이지의 수동 재수집 버튼과 호환되는 실제 수집 실행 API. */
    @PostMapping("/collect")
    public Map<String, String> runManualCollect() {
        AutoCollectionRunResponse result = autoDataCollectionService.runManualCollectionCycle();
        return Map.of(
                "message",
                result.getMessage()
                        + " / 최신 패치 " + result.getLatestPatch()
                        + " 수집 경기 " + result.getLatestPatchMatchCount()
                        + "개"
        );
    }
}
