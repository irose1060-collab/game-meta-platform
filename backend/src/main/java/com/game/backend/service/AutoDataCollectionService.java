package com.game.backend.service;

import com.game.backend.dto.AutoCollectionRunResponse;
import com.game.backend.dto.CollectionResultResponse;
import com.game.backend.dto.StatsRebuildResponse;
import com.game.backend.entity.DataCollectionLog;
import com.game.backend.entity.SeedPlayer;
import com.game.backend.repository.DataCollectionLogRepository;
import com.game.backend.repository.SeedPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoDataCollectionService {

    private static final int SOLO_RANK_QUEUE_ID = 420;

    private final SeedPlayerRepository seedPlayerRepository;
    private final RiotDataCollectionService riotDataCollectionService;
    private final ChampionStatsService championStatsService;
    private final DataCollectionLogRepository dataCollectionLogRepository;

    @Value("${riot.auto-collect.enabled:false}")
    private boolean autoCollectEnabled;

    @Value("${riot.auto-collect.match-count-per-player:5}")
    private int matchCountPerPlayer;

    @Value("${riot.auto-collect.max-players-per-cycle:3}")
    private int maxPlayersPerCycle;

    @Value("${riot.auto-collect.delay-between-players-ms:3000}")
    private long delayBetweenPlayersMs;

    @Value("${riot.auto-collect.target-games-per-patch:1000}")
    private int targetGamesPerPatch;

    /**
     * 이전 자동수집이 끝난 뒤 설정된 시간 후 다시 실행한다.
     * 기본값은 application.properties에서 1시간으로 조정했다.
     */
    @Scheduled(fixedDelayString = "${riot.auto-collect.fixed-delay-ms:3600000}")
    public void runAutoCollection() {
        if (!autoCollectEnabled) {
            return;
        }

        try {
            runCollectionCycle(false);
        } catch (Exception e) {
            log.error("[AUTO COLLECT] 자동수집 전체 실패: {}", e.getMessage(), e);
        }
    }

    /** 관리자 화면/운영자가 즉시 실행할 수 있는 수동 수집 사이클. */
    public AutoCollectionRunResponse runManualCollectionCycle() {
        return runCollectionCycle(true);
    }

    public AutoCollectionRunResponse runCollectionCycle(boolean manual) {
        String jobName = manual ? "manual_current_patch_collection" : "auto_current_patch_collection";
        LocalDateTime startedAt = LocalDateTime.now();

        int processed = 0;
        int saved = 0;
        int skipped = 0;
        int failed = 0;
        int participants = 0;
        int rebuiltRows = 0;
        boolean success = true;
        String message = "수집 완료";

        try {
            List<SeedPlayer> seeds = seedPlayerRepository.findByEnabledTrueOrderByLastCollectedAtAsc();

            if (seeds.isEmpty()) {
                message = "활성화된 seed player가 없습니다.";
                LocalDateTime endedAt = LocalDateTime.now();
                saveLog(jobName, "SUCCESS", startedAt, endedAt, 0, 0, 0, message);
                return buildResponse(true, manual, jobName, 0, 0, 0, 0, 0, 0, startedAt, endedAt, message);
            }

            int limit = Math.min(Math.max(1, maxPlayersPerCycle), seeds.size());
            log.info("[AUTO COLLECT] {} 시작: 대상 {}명 / 전체 seed {}명", jobName, limit, seeds.size());

            for (int i = 0; i < limit; i++) {
                SeedPlayer seed = seeds.get(i);

                try {
                    CollectionResultResponse result = collectOneSeed(seed);
                    processed++;
                    saved += result.getSavedMatchCount();
                    skipped += result.getSkippedExistingMatchCount();
                    failed += result.getFailedMatchCount();
                    participants += result.getSavedParticipantCount();
                } catch (Exception e) {
                    failed++;
                    markFailed(seed, e.getMessage());
                    log.error("[AUTO COLLECT] {}#{} 수집 실패: {}", seed.getGameName(), seed.getTagLine(), e.getMessage(), e);
                }

                if (i < limit - 1) {
                    sleep(delayBetweenPlayersMs);
                }
            }

            try {
                StatsRebuildResponse rebuild = championStatsService.rebuildChampionStats();
                rebuiltRows = rebuild.getRebuiltCount();
                log.info("[AUTO COLLECT] 최신 패치 챔피언 통계 재집계 완료: {}", rebuild.getMessage());
            } catch (Exception e) {
                success = false;
                message = "수집은 완료됐지만 통계 재집계 실패: " + e.getMessage();
                log.error("[AUTO COLLECT] 챔피언 통계 재집계 실패: {}", e.getMessage(), e);
            }

            if (success) {
                message = "수집 완료: 처리 seed " + processed
                        + "명, 신규 경기 " + saved
                        + "개, 스킵 " + skipped
                        + "개, 실패 " + failed
                        + "개, 통계 row " + rebuiltRows
                        + "개 재집계";
            }

            LocalDateTime endedAt = LocalDateTime.now();
            saveLog(jobName, success ? "SUCCESS" : "PARTIAL_FAIL", startedAt, endedAt, processed, saved, failed, message);
            return buildResponse(success, manual, jobName, processed, saved, skipped, failed, participants, rebuiltRows, startedAt, endedAt, message);
        } catch (Exception e) {
            success = false;
            message = "수집 실패: " + e.getMessage();
            LocalDateTime endedAt = LocalDateTime.now();
            saveLog(jobName, "FAIL", startedAt, endedAt, processed, saved, failed + 1, message);
            log.error("[AUTO COLLECT] {} 실패: {}", jobName, e.getMessage(), e);
            return buildResponse(false, manual, jobName, processed, saved, skipped, failed + 1, participants, rebuiltRows, startedAt, endedAt, message);
        }
    }

    @Transactional
    public SeedPlayer addSeedPlayer(String gameName, String tagLine) {
        String cleanGameName = gameName.trim();
        String cleanTagLine = tagLine.trim();

        return seedPlayerRepository
                .findByGameNameIgnoreCaseAndTagLineIgnoreCase(cleanGameName, cleanTagLine)
                .orElseGet(() -> seedPlayerRepository.save(
                        SeedPlayer.builder()
                                .gameName(cleanGameName)
                                .tagLine(cleanTagLine)
                                .enabled(true)
                                .totalSavedMatches(0)
                                .totalFailedCount(0)
                                .createdAt(LocalDateTime.now())
                                .build()
                ));
    }

    @Transactional
    public SeedPlayer toggleSeedPlayer(Long id, boolean enabled) {
        SeedPlayer seed = seedPlayerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seed player를 찾을 수 없습니다. id=" + id));

        seed.setEnabled(enabled);
        return seed;
    }

    public List<SeedPlayer> getSeedPlayers() {
        return seedPlayerRepository.findAll();
    }

    private CollectionResultResponse collectOneSeed(SeedPlayer seed) {
        log.info("[AUTO COLLECT] {}#{} 수집 시작", seed.getGameName(), seed.getTagLine());

        CollectionResultResponse result = riotDataCollectionService.collectMatches(
                seed.getGameName(),
                seed.getTagLine(),
                matchCountPerPlayer
        );

        seed.setLastCollectedAt(LocalDateTime.now());
        seed.setLastResultMessage(result.getMessage());
        seed.setTotalSavedMatches(seed.getTotalSavedMatches() + result.getSavedMatchCount());

        seedPlayerRepository.save(seed);

        log.info("[AUTO COLLECT] {}#{} 수집 완료: 신규 경기 {}개 / 스킵 {}개 / 실패 {}개",
                seed.getGameName(),
                seed.getTagLine(),
                result.getSavedMatchCount(),
                result.getSkippedExistingMatchCount(),
                result.getFailedMatchCount()
        );

        return result;
    }

    private void markFailed(SeedPlayer seed, String message) {
        seed.setLastCollectedAt(LocalDateTime.now());
        seed.setLastResultMessage("수집 실패: " + message);
        seed.setTotalFailedCount(seed.getTotalFailedCount() + 1);
        seedPlayerRepository.save(seed);
    }

    private void saveLog(
            String jobName,
            String status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            int totalCount,
            int successCount,
            int failCount,
            String errorMessage
    ) {
        try {
            dataCollectionLogRepository.save(
                    DataCollectionLog.builder()
                            .jobName(jobName)
                            .status(status)
                            .startedAt(startedAt)
                            .endedAt(endedAt)
                            .totalCount(totalCount)
                            .successCount(successCount)
                            .failCount(failCount)
                            .errorMessage(errorMessage)
                            .build()
            );
        } catch (Exception e) {
            log.warn("[AUTO COLLECT] 수집 로그 저장 실패: {}", e.getMessage());
        }
    }

    private AutoCollectionRunResponse buildResponse(
            boolean success,
            boolean manual,
            String jobName,
            int processed,
            int saved,
            int skipped,
            int failed,
            int participants,
            int rebuiltRows,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String message
    ) {
        String latestPatch = championStatsService.resolveLatestMatchPatch(SOLO_RANK_QUEUE_ID);
        long latestPatchMatchCount = championStatsService.countMatchesForPatch(latestPatch, SOLO_RANK_QUEUE_ID);
        long latestPatchTotalGames = championStatsService.sumGamesForPatch(latestPatch, SOLO_RANK_QUEUE_ID);
        double progress = targetGamesPerPatch <= 0
                ? 0.0
                : Math.min(100.0, Math.round(latestPatchMatchCount * 10000.0 / targetGamesPerPatch) / 100.0);

        return AutoCollectionRunResponse.builder()
                .success(success)
                .manual(manual)
                .jobName(jobName)
                .latestPatch(latestPatch)
                .targetGameCount(targetGamesPerPatch)
                .latestPatchMatchCount(latestPatchMatchCount)
                .latestPatchTotalGames(latestPatchTotalGames)
                .latestPatchProgressPercent(progress)
                .processedSeedCount(processed)
                .savedMatchCount(saved)
                .skippedExistingMatchCount(skipped)
                .failedMatchCount(failed)
                .savedParticipantCount(participants)
                .rebuiltStatCount(rebuiltRows)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .message(message)
                .build();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("자동 수집 대기 중 인터럽트가 발생했습니다.", e);
        }
    }
}
