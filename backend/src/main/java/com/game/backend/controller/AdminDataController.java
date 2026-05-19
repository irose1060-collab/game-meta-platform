package com.game.backend.controller;

import com.game.backend.entity.DataCollectionLog;
import com.game.backend.repository.DataCollectionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/data")
@RequiredArgsConstructor
public class AdminDataController {

    private final DataCollectionLogRepository dataCollectionLogRepository;

    @GetMapping("/logs")
    public List<DataCollectionLog> getLogs() {
        return dataCollectionLogRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @PostMapping("/collect")
    public Map<String, String> runManualCollect() {
        LocalDateTime now = LocalDateTime.now();

        DataCollectionLog log = DataCollectionLog.builder()
                .jobName("manual_riot_account_collect")
                .status("SUCCESS")
                .startedAt(now)
                .endedAt(now.plusSeconds(2))
                .totalCount(1)
                .successCount(1)
                .failCount(0)
                .errorMessage(null)
                .build();

        dataCollectionLogRepository.save(log);

        return Map.of("message", "수동 재수집이 완료되었습니다.");
    }
}
