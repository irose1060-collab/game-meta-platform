package com.game.backend.service;

import com.game.backend.dto.*;
import com.game.backend.entity.ChallengerRankingPlayer;
import com.game.backend.repository.ChallengerRankingPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengerRankingService {

    private static final String SOLO_RANK_QUEUE = "RANKED_SOLO_5x5";

    private final RestTemplate restTemplate;
    private final ChallengerRankingPlayerRepository rankingRepository;
    private final AutoDataCollectionService autoDataCollectionService;
    private final RiotDataCollectionService riotDataCollectionService;

    @Value("${riot.api.key}")
    private String riotApiKey;

    @Value("${riot.platform.base-url:https://kr.api.riotgames.com}")
    private String platformBaseUrl;

    @Value("${riot.account.base-url:https://asia.api.riotgames.com}")
    private String accountBaseUrl;

    @Value("${riot.ranking.account-delay-ms:1400}")
    private long accountDelayMs;

    @Value("${riot.ranking.collect-delay-ms:3000}")
    private long collectDelayMs;

    @Transactional
    public RankingRefreshResponse refreshKrChallengerRanking(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        ChallengerLeagueResponse league = fetchChallengerLeague();

        if (league.getEntries() == null || league.getEntries().isEmpty()) {
            return RankingRefreshResponse.builder()
                    .requestedLimit(safeLimit)
                    .savedCount(0)
                    .seedAddedCount(0)
                    .message("KR 챌린저 랭킹 응답이 비어 있습니다.")
                    .players(List.of())
                    .build();
        }

        List<ChallengerLeagueEntryResponse> topEntries = league.getEntries().stream()
                .filter(entry -> entry.getPuuid() != null && !entry.getPuuid().isBlank())
                .sorted(
                        Comparator.comparingInt(ChallengerLeagueEntryResponse::getLeaguePoints).reversed()
                                .thenComparing(Comparator.comparingInt(ChallengerLeagueEntryResponse::getWins).reversed())
                                .thenComparingInt(ChallengerLeagueEntryResponse::getLosses)
                )
                .limit(safeLimit)
                .toList();

        int savedCount = 0;
        int seedAddedCount = 0;

        for (int i = 0; i < topEntries.size(); i++) {
            ChallengerLeagueEntryResponse entry = topEntries.get(i);
            int rankNo = i + 1;

            RiotAccountResponse account = resolveAccountByPuuid(entry.getPuuid(), entry);

            double winRate = calculateWinRate(entry.getWins(), entry.getLosses());

            ChallengerRankingPlayer player = rankingRepository.findByPuuid(entry.getPuuid())
                    .orElseGet(() -> ChallengerRankingPlayer.builder()
                            .puuid(entry.getPuuid())
                            .build());

            player.setRankNo(rankNo);
            player.setQueueType(SOLO_RANK_QUEUE);
            player.setGameName(account.getGameName());
            player.setTagLine(account.getTagLine());
            player.setLeaguePoints(entry.getLeaguePoints());
            player.setWins(entry.getWins());
            player.setLosses(entry.getLosses());
            player.setWinRate(winRate);
            player.setLastRefreshedAt(LocalDateTime.now());

            if (account.getGameName() != null && account.getTagLine() != null) {
                autoDataCollectionService.addSeedPlayer(account.getGameName(), account.getTagLine());
                player.setSeedAdded(true);
                seedAddedCount++;
            }

            rankingRepository.save(player);
            savedCount++;

            sleepQuietly(accountDelayMs);
        }

        List<RankingPlayerResponse> players = getTopRankingPlayers(safeLimit);

        return RankingRefreshResponse.builder()
                .requestedLimit(safeLimit)
                .savedCount(savedCount)
                .seedAddedCount(seedAddedCount)
                .players(players)
                .message("KR 챌린저 랭킹 " + savedCount + "명 저장 및 seed 등록 완료")
                .build();
    }

    public List<RankingPlayerResponse> getTopRankingPlayers(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        return rankingRepository.findByQueueTypeOrderByRankNoAsc(SOLO_RANK_QUEUE).stream()
                .limit(safeLimit)
                .map(RankingPlayerResponse::from)
                .toList();
    }

    public RankingCollectResponse collectMatchesFromRankingPlayers(int limit, int count) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeCount = Math.max(1, Math.min(count, 10));

        List<ChallengerRankingPlayer> players = rankingRepository
                .findByQueueTypeOrderByRankNoAsc(SOLO_RANK_QUEUE)
                .stream()
                .filter(player -> player.getGameName() != null && player.getTagLine() != null)
                .limit(safeLimit)
                .toList();

        int processed = 0;
        int totalSaved = 0;
        int totalSkipped = 0;
        int totalFailed = 0;
        int totalParticipants = 0;

        for (ChallengerRankingPlayer player : players) {
            try {
                log.info("[RANKING COLLECT] {}위 {}#{} 전적 수집 시작",
                        player.getRankNo(),
                        player.getGameName(),
                        player.getTagLine()
                );

                CollectionResultResponse result = riotDataCollectionService.collectRecentMatchesByRiotId(
                        player.getGameName(),
                        player.getTagLine(),
                        safeCount
                );

                processed++;
                totalSaved += result.getSavedMatchCount();
                totalSkipped += result.getSkippedExistingMatchCount();
                totalFailed += result.getFailedMatchCount();
                totalParticipants += result.getSavedParticipantCount();

                log.info("[RANKING COLLECT] {}#{} 완료: 신규 {} / 스킵 {} / 실패 {}",
                        player.getGameName(),
                        player.getTagLine(),
                        result.getSavedMatchCount(),
                        result.getSkippedExistingMatchCount(),
                        result.getFailedMatchCount()
                );

            } catch (Exception e) {
                totalFailed++;
                log.error("[RANKING COLLECT] {}#{} 실패: {}",
                        player.getGameName(),
                        player.getTagLine(),
                        e.getMessage()
                );
            }

            sleepQuietly(collectDelayMs);
        }

        return RankingCollectResponse.builder()
                .requestedPlayerLimit(safeLimit)
                .matchCountPerPlayer(safeCount)
                .processedPlayerCount(processed)
                .totalSavedMatchCount(totalSaved)
                .totalSkippedExistingMatchCount(totalSkipped)
                .totalFailedMatchCount(totalFailed)
                .totalSavedParticipantCount(totalParticipants)
                .message("랭킹 유저 기반 전적 수집 완료")
                .build();
    }

    private ChallengerLeagueResponse fetchChallengerLeague() {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(platformBaseUrl)
                .pathSegment("lol", "league", "v4", "challengerleagues", "by-queue", SOLO_RANK_QUEUE)
                .build()
                .encode()
                .toUri();

        HttpEntity<Void> entity = new HttpEntity<>(riotHeaders());

        ResponseEntity<ChallengerLeagueResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                ChallengerLeagueResponse.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("챌린저 랭킹 응답이 비어 있습니다.");
        }

        return response.getBody();
    }

    private RiotAccountResponse resolveAccountByPuuid(
            String puuid,
            ChallengerLeagueEntryResponse entry
    ) {
        if (entry.getGameName() != null && entry.getTagLine() != null) {
            RiotAccountResponse response = new RiotAccountResponse();
            response.setPuuid(puuid);
            response.setGameName(entry.getGameName());
            response.setTagLine(entry.getTagLine());
            return response;
        }

        URI uri = UriComponentsBuilder
                .fromHttpUrl(accountBaseUrl)
                .pathSegment("riot", "account", "v1", "accounts", "by-puuid", puuid)
                .build()
                .encode()
                .toUri();

        HttpEntity<Void> entity = new HttpEntity<>(riotHeaders());

        ResponseEntity<RiotAccountResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                RiotAccountResponse.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("PUUID로 Riot 계정 정보를 찾지 못했습니다. puuid=" + puuid);
        }

        return response.getBody();
    }

    private HttpHeaders riotHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey.trim());
        return headers;
    }

    private double calculateWinRate(int wins, int losses) {
        int total = wins + losses;
        if (total == 0) {
            return 0.0;
        }

        return Math.round((wins * 10000.0 / total)) / 100.0;
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}