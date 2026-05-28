package com.game.backend.service;

import com.game.backend.dto.CollectionResultResponse;
import com.game.backend.entity.SeedPlayer;
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

    private final SeedPlayerRepository seedPlayerRepository;
    private final RiotDataCollectionService riotDataCollectionService;
    private final ChampionStatsService championStatsService;

    @Value("${riot.auto-collect.enabled:false}")
    private boolean autoCollectEnabled;

    @Value("${riot.auto-collect.match-count-per-player:5}")
    private int matchCountPerPlayer;

    @Value("${riot.auto-collect.max-players-per-cycle:3}")
    private int maxPlayersPerCycle;

    @Value("${riot.auto-collect.delay-between-players-ms:3000}")
    private long delayBetweenPlayersMs;

    /*
     * fixedDelay 기준:
     * 이전 자동수집이 끝난 뒤 10분 후 다시 실행.
     * application.properties에서 변경 가능.
     */
    @Scheduled(fixedDelayString = "${riot.auto-collect.fixed-delay-ms:600000}")
    public void runAutoCollection() {
        if (!autoCollectEnabled) {
            return;
        }

        List<SeedPlayer> seeds = seedPlayerRepository.findByEnabledTrueOrderByLastCollectedAtAsc();

        if (seeds.isEmpty()) {
            log.info("[AUTO COLLECT] 활성화된 seed player가 없습니다.");
            return;
        }

        int limit = Math.min(maxPlayersPerCycle, seeds.size());

        log.info("[AUTO COLLECT] 자동수집 시작: 대상 {}명 / 전체 seed {}명", limit, seeds.size());

        for (int i = 0; i < limit; i++) {
            SeedPlayer seed = seeds.get(i);

            try {
                collectOneSeed(seed);

                if (i < limit - 1) {
                    Thread.sleep(delayBetweenPlayersMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[AUTO COLLECT] 자동수집 sleep 중단");
                return;
            } catch (Exception e) {
                log.error("[AUTO COLLECT] {}#{} 수집 실패: {}",
                        seed.getGameName(),
                        seed.getTagLine(),
                        e.getMessage()
                );

                markFailed(seed, e.getMessage());
            }
        }

        try {
            championStatsService.rebuildChampionStats();
            log.info("[AUTO COLLECT] 챔피언 통계 재집계 완료");
        } catch (Exception e) {
            log.error("[AUTO COLLECT] 챔피언 통계 재집계 실패: {}", e.getMessage());
        }

        log.info("[AUTO COLLECT] 자동수집 종료");
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

    private void collectOneSeed(SeedPlayer seed) {
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
    }

    private void markFailed(SeedPlayer seed, String message) {
        seed.setLastCollectedAt(LocalDateTime.now());
        seed.setLastResultMessage("수집 실패: " + message);
        seed.setTotalFailedCount(seed.getTotalFailedCount() + 1);
        seedPlayerRepository.save(seed);
    }
}