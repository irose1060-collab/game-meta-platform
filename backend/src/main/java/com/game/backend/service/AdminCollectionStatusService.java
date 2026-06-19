package com.game.backend.service;

import com.game.backend.dto.AdminCollectionStatusResponse;
import com.game.backend.dto.AdminSeedStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCollectionStatusService {

    private final JdbcTemplate jdbcTemplate;
    private final ChampionStatsService championStatsService;

    @Value("${riot.auto-collect.enabled:false}")
    private boolean autoCollectEnabled;

    @Value("${riot.auto-collect.match-count-per-player:5}")
    private int matchCountPerPlayer;

    @Value("${riot.auto-collect.max-players-per-cycle:3}")
    private int maxPlayersPerCycle;

    @Value("${riot.auto-collect.fixed-delay-ms:600000}")
    private long fixedDelayMs;

    @Value("${riot.auto-collect.delay-between-players-ms:3000}")
    private long delayBetweenPlayersMs;

    @Value("${riot.auto-collect.target-games-per-patch:1000}")
    private int targetPatchGames;

    public AdminCollectionStatusResponse getStatus() {
        String latestPatch = championStatsService.resolveLatestMatchPatch(420);
        long latestPatchMatchCount = championStatsService.countMatchesForPatch(latestPatch, 420);
        long latestPatchStatRows = championStatsService.countStatRowsForPatch(latestPatch, 420);
        long latestPatchTotalGames = championStatsService.sumGamesForPatch(latestPatch, 420);
        double latestPatchProgressPercent = targetPatchGames <= 0
                ? 0.0
                : Math.min(100.0, Math.round(latestPatchMatchCount * 10000.0 / targetPatchGames) / 100.0);

        return AdminCollectionStatusResponse.builder()
                .matchCount(count("matches"))
                .participantCount(count("match_participants"))
                .championStatCount(count("champion_stats"))
                .seedPlayerCount(countIfExists("seed_players"))
                .enabledSeedPlayerCount(countEnabledSeeds())
                .rankingPlayerCount(countIfExists("challenger_ranking_players"))
                .totalSavedMatchesBySeeds(sumTotalSavedMatches())
                .failedSeedCount(countFailedSeeds())
                .latestPatch(latestPatch)
                .latestPatchMatchCount(latestPatchMatchCount)
                .latestPatchStatRows(latestPatchStatRows)
                .latestPatchTotalGames(latestPatchTotalGames)
                .targetPatchGames(targetPatchGames)
                .latestPatchProgressPercent(latestPatchProgressPercent)
                .lastCollectedAt(maxDateTime("seed_players", "last_collected_at"))
                .lastMatchCreatedAt(maxDateTime("matches", "created_at"))
                .lastStatsUpdatedAt(maxDateTime("champion_stats", "updated_at"))
                .autoCollectEnabled(autoCollectEnabled)
                .matchCountPerPlayer(matchCountPerPlayer)
                .maxPlayersPerCycle(maxPlayersPerCycle)
                .fixedDelayMs(fixedDelayMs)
                .delayBetweenPlayersMs(delayBetweenPlayersMs)
                .recentSeeds(findRecentSeeds())
                .build();
    }

    private long count(String tableName) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Long.class
        );
        return value == null ? 0 : value;
    }

    private long countIfExists(String tableName) {
        try {
            return count(tableName);
        } catch (Exception e) {
            return 0;
        }
    }

    private long countEnabledSeeds() {
        try {
            Long value = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM seed_players WHERE enabled = true",
                    Long.class
            );
            return value == null ? 0 : value;
        } catch (Exception e) {
            return 0;
        }
    }

    private long countFailedSeeds() {
        try {
            Long value = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM seed_players WHERE total_failed_count > 0",
                    Long.class
            );
            return value == null ? 0 : value;
        } catch (Exception e) {
            return 0;
        }
    }

    private long sumTotalSavedMatches() {
        try {
            Long value = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(total_saved_matches), 0) FROM seed_players",
                    Long.class
            );
            return value == null ? 0 : value;
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalDateTime maxDateTime(String tableName, String columnName) {
        try {
            Timestamp timestamp = jdbcTemplate.queryForObject(
                    "SELECT MAX(" + columnName + ") FROM " + tableName,
                    Timestamp.class
            );

            if (timestamp == null) {
                return null;
            }

            return timestamp.toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private List<AdminSeedStatusResponse> findRecentSeeds() {
        try {
            String sql = """
                    SELECT
                        id,
                        game_name,
                        tag_line,
                        enabled,
                        last_collected_at,
                        total_saved_matches,
                        total_failed_count,
                        last_result_message
                    FROM seed_players
                    ORDER BY last_collected_at DESC NULLS LAST, id ASC
                    LIMIT 20
                    """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Timestamp lastCollectedAt = rs.getTimestamp("last_collected_at");

                return AdminSeedStatusResponse.builder()
                        .id(rs.getLong("id"))
                        .gameName(rs.getString("game_name"))
                        .tagLine(rs.getString("tag_line"))
                        .enabled(rs.getBoolean("enabled"))
                        .lastCollectedAt(
                                lastCollectedAt == null
                                        ? null
                                        : lastCollectedAt.toLocalDateTime()
                        )
                        .totalSavedMatches(rs.getInt("total_saved_matches"))
                        .totalFailedCount(rs.getInt("total_failed_count"))
                        .lastResultMessage(rs.getString("last_result_message"))
                        .build();
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}