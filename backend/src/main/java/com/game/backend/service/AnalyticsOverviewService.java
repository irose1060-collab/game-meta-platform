package com.game.backend.service;

import com.game.backend.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsOverviewService {

    private static final List<String> POSITIONS = List.of(
            "TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY"
    );

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsOverviewResponse getOverview(String patch, int minGames) {
        int safeMinGames = Math.max(1, Math.min(minGames, 100));
        String targetPatch = resolvePatch(patch);

        AnalyticsSummaryResponse summary = getSummary(targetPatch, safeMinGames);

        return AnalyticsOverviewResponse.builder()
                .summary(summary)
                .positionDistribution(getPositionDistribution(targetPatch))
                .positionTopChampions(getPositionTopChampions(targetPatch, safeMinGames))
                .topWinRateChampions(getTopChampions(targetPatch, safeMinGames, "win_rate DESC, games DESC", 10))
                .topPickRateChampions(getTopChampions(targetPatch, safeMinGames, "pick_rate DESC, games DESC", 10))
                .topDamageChampions(getTopChampions(targetPatch, safeMinGames, "avg_damage DESC, games DESC", 10))
                .topKdaChampions(getTopChampions(targetPatch, safeMinGames, "avg_kda DESC, games DESC", 10))
                .scatterChampions(getTopChampions(targetPatch, safeMinGames, "games DESC, tier_score DESC", 140))
                .build();
    }

    private String resolvePatch(String patch) {
        if (patch != null && !patch.isBlank()) {
            return patch.trim();
        }

        String sql = """
                SELECT patch
                FROM champion_stats
                WHERE patch IS NOT NULL
                  AND patch ~ '^\\d+\\.\\d+$'
                GROUP BY patch
                ORDER BY
                    SPLIT_PART(patch, '.', 1)::int DESC,
                    SPLIT_PART(patch, '.', 2)::int DESC
                LIMIT 1
                """;

        List<String> result = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("patch"));
        if (!result.isEmpty()) {
            return result.get(0);
        }

        List<String> fallback = jdbcTemplate.query(
                "SELECT patch FROM champion_stats WHERE patch IS NOT NULL GROUP BY patch ORDER BY patch DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("patch")
        );

        return fallback.isEmpty() ? "" : fallback.get(0);
    }

    private AnalyticsSummaryResponse getSummary(String patch, int minGames) {
        long matchCount = count("matches");
        long participantCount = count("match_participants");
        long championStatCount = count("champion_stats");
        long seedPlayerCount = countIfExists("seed_players");
        long rankingPlayerCount = countIfExists("challenger_ranking_players");

        Long analyzedPickCount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(games), 0) FROM champion_stats WHERE patch = ?",
                Long.class,
                patch
        );

        return AnalyticsSummaryResponse.builder()
                .matchCount(matchCount)
                .participantCount(participantCount)
                .championStatCount(championStatCount)
                .seedPlayerCount(seedPlayerCount)
                .rankingPlayerCount(rankingPlayerCount)
                .latestPatch(patch)
                .analyzedPickCount(analyzedPickCount == null ? 0 : analyzedPickCount)
                .minGames(minGames)
                .build();
    }

    private long count(String tableName) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return value == null ? 0 : value;
    }

    private long countIfExists(String tableName) {
        try {
            return count(tableName);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private List<PositionDistributionResponse> getPositionDistribution(String patch) {
        String sql = """
                SELECT
                    mp.team_position AS position,
                    COUNT(*) AS pick_count
                FROM match_participants mp
                JOIN matches m ON m.match_id = mp.match_id
                WHERE m.queue_id = 420
                  AND CONCAT(SPLIT_PART(m.game_version, '.', 1), '.', SPLIT_PART(m.game_version, '.', 2)) = ?
                  AND mp.team_position IN ('TOP', 'JUNGLE', 'MIDDLE', 'BOTTOM', 'UTILITY')
                GROUP BY mp.team_position
                """;

        Map<String, Long> countMap = new LinkedHashMap<>();
        for (String position : POSITIONS) {
            countMap.put(position, 0L);
        }

        jdbcTemplate.query(sql, rs -> {
            countMap.put(rs.getString("position"), rs.getLong("pick_count"));
        }, patch);

        long total = countMap.values().stream().mapToLong(Long::longValue).sum();

        List<PositionDistributionResponse> result = new ArrayList<>();
        for (String position : POSITIONS) {
            long pickCount = countMap.getOrDefault(position, 0L);
            double percentage = total == 0 ? 0.0 : Math.round((pickCount * 10000.0 / total)) / 100.0;

            result.add(PositionDistributionResponse.builder()
                    .position(position)
                    .pickCount(pickCount)
                    .percentage(percentage)
                    .build());
        }

        return result;
    }

    private List<AnalyticsChampionResponse> getTopChampions(
            String patch,
            int minGames,
            String orderBy,
            int limit
    ) {
        String sql = """
            SELECT
                patch,
                queue_id,
                position,
                champion_id,
                champion_name,
                games,
                wins,
                win_rate,
                pick_rate,
                avg_kda,
                avg_damage,
                avg_gold,
                avg_cs,
                avg_vision_score,
                tier_score,
                tier
            FROM champion_stats
            WHERE patch = ?
              AND games >= ?
            ORDER BY 
            """ + orderBy + """
            
            LIMIT ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapChampion(rs),
                patch,
                minGames,
                limit
        );
    }

    private List<PositionTopChampionsResponse> getPositionTopChampions(String patch, int minGames) {
        String sql = """
                SELECT *
                FROM (
                    SELECT
                        patch,
                        queue_id,
                        position,
                        champion_id,
                        champion_name,
                        games,
                        wins,
                        win_rate,
                        pick_rate,
                        avg_kda,
                        avg_damage,
                        avg_gold,
                        avg_cs,
                        avg_vision_score,
                        tier_score,
                        tier,
                        ROW_NUMBER() OVER (
                            PARTITION BY position
                            ORDER BY tier_score DESC, games DESC
                        ) AS rn
                    FROM champion_stats
                    WHERE patch = ?
                      AND games >= ?
                      AND position IN ('TOP', 'JUNGLE', 'MIDDLE', 'BOTTOM', 'UTILITY')
                ) ranked
                WHERE rn <= 5
                ORDER BY position, rn
                """;

        Map<String, List<AnalyticsChampionResponse>> grouped = new LinkedHashMap<>();
        for (String position : POSITIONS) {
            grouped.put(position, new ArrayList<>());
        }

        jdbcTemplate.query(sql, rs -> {
            String position = rs.getString("position");
            grouped.computeIfAbsent(position, key -> new ArrayList<>()).add(mapChampion(rs));
        }, patch, minGames);

        List<PositionTopChampionsResponse> result = new ArrayList<>();
        for (String position : POSITIONS) {
            result.add(PositionTopChampionsResponse.builder()
                    .position(position)
                    .champions(grouped.getOrDefault(position, List.of()))
                    .build());
        }

        return result;
    }

    private AnalyticsChampionResponse mapChampion(ResultSet rs) throws SQLException {
        return AnalyticsChampionResponse.builder()
                .patch(rs.getString("patch"))
                .queueId(rs.getInt("queue_id"))
                .position(rs.getString("position"))
                .championId(rs.getInt("champion_id"))
                .championName(rs.getString("champion_name"))
                .games(rs.getInt("games"))
                .wins(rs.getInt("wins"))
                .winRate(rs.getDouble("win_rate"))
                .pickRate(rs.getDouble("pick_rate"))
                .avgKda(rs.getDouble("avg_kda"))
                .avgDamage(rs.getDouble("avg_damage"))
                .avgGold(rs.getDouble("avg_gold"))
                .avgCs(rs.getDouble("avg_cs"))
                .avgVisionScore(rs.getDouble("avg_vision_score"))
                .tierScore(rs.getDouble("tier_score"))
                .tier(rs.getString("tier"))
                .build();
    }
}
