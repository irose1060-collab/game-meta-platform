package com.game.backend.service;

import com.game.backend.dto.ChampionStatResponse;
import com.game.backend.dto.StatsRebuildResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChampionStatsService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public StatsRebuildResponse rebuildChampionStats() {
        jdbcTemplate.update("DELETE FROM champion_stats");

        String insertSql = """
                INSERT INTO champion_stats (
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
                    updated_at
                )
                WITH base AS (
                    SELECT
                        COALESCE(
                            NULLIF(
                                CONCAT(
                                    SPLIT_PART(COALESCE(m.game_version, 'unknown'), '.', 1),
                                    '.',
                                    SPLIT_PART(COALESCE(m.game_version, 'unknown'), '.', 2)
                                ),
                                '.'
                            ),
                            'unknown'
                        ) AS patch,
                        m.queue_id,
                        COALESCE(NULLIF(mp.team_position, ''), 'UNKNOWN') AS position,
                        mp.champion_id,
                        mp.champion_name,
                        COALESCE(mp.win, false) AS win,
                        COALESCE(mp.kills, 0) AS kills,
                        COALESCE(mp.deaths, 0) AS deaths,
                        COALESCE(mp.assists, 0) AS assists,
                        COALESCE(mp.total_damage_dealt_to_champions, 0) AS damage,
                        COALESCE(mp.gold_earned, 0) AS gold,
                        COALESCE(mp.total_minions_killed, 0)
                            + COALESCE(mp.neutral_minions_killed, 0) AS cs,
                        COALESCE(mp.vision_score, 0) AS vision_score
                    FROM match_participants mp
                    JOIN matches m ON m.match_id = mp.match_id
                    WHERE m.queue_id = 420
                      AND mp.champion_id IS NOT NULL
                      AND mp.champion_name IS NOT NULL
                      AND mp.champion_name <> ''
                      AND mp.team_position IS NOT NULL
                      AND mp.team_position <> ''
                      AND mp.team_position IN ('TOP', 'JUNGLE', 'MIDDLE', 'BOTTOM', 'UTILITY')
                ),
                position_totals AS (
                    SELECT
                        patch,
                        queue_id,
                        position,
                        COUNT(*) AS total_picks
                    FROM base
                    GROUP BY patch, queue_id, position
                ),
                agg AS (
                    SELECT
                        b.patch,
                        b.queue_id,
                        b.position,
                        b.champion_id,
                        b.champion_name,
                        COUNT(*) AS games,
                        SUM(CASE WHEN b.win = true THEN 1 ELSE 0 END) AS wins,
                        ROUND(
                            (
                                SUM(CASE WHEN b.win = true THEN 1 ELSE 0 END)::numeric
                                / NULLIF(COUNT(*)::numeric, 0)
                            ) * 100,
                            2
                        ) AS win_rate,
                        ROUND(
                            (
                                COUNT(*)::numeric
                                / NULLIF(MAX(pt.total_picks)::numeric, 0)
                            ) * 100,
                            2
                        ) AS pick_rate,
                        ROUND(
                            AVG(
                                (b.kills + b.assists)::numeric
                                / GREATEST(b.deaths, 1)
                            ),
                            2
                        ) AS avg_kda,
                        ROUND(AVG(b.damage)::numeric, 2) AS avg_damage,
                        ROUND(AVG(b.gold)::numeric, 2) AS avg_gold,
                        ROUND(AVG(b.cs)::numeric, 2) AS avg_cs,
                        ROUND(AVG(b.vision_score)::numeric, 2) AS avg_vision_score
                    FROM base b
                    JOIN position_totals pt
                      ON pt.patch = b.patch
                     AND pt.queue_id = b.queue_id
                     AND pt.position = b.position
                    GROUP BY
                        b.patch,
                        b.queue_id,
                        b.position,
                        b.champion_id,
                        b.champion_name
                ),
                scored AS (
                    SELECT
                        a.*,
                        ROUND(
                            (
                                (a.win_rate * 0.45)
                                + (a.pick_rate * 2.50)
                                + (a.avg_kda * 2.00)
                                + (LN((a.games + 1)::numeric) * 2.00)
                            )::numeric,
                            2
                        ) AS tier_score
                    FROM agg a
                )
                SELECT
                    patch,
                    queue_id,
                    position,
                    champion_id,
                    champion_name,
                    games::int,
                    wins::int,
                    win_rate::double precision,
                    pick_rate::double precision,
                    avg_kda::double precision,
                    avg_damage::double precision,
                    avg_gold::double precision,
                    avg_cs::double precision,
                    avg_vision_score::double precision,
                    tier_score::double precision,
                    CASE
                        WHEN games < 3 THEN 'N/A'
                        WHEN tier_score >= 35 THEN 'S'
                        WHEN tier_score >= 30 THEN 'A'
                        WHEN tier_score >= 25 THEN 'B'
                        WHEN tier_score >= 20 THEN 'C'
                        ELSE 'D'
                    END AS tier,
                    CURRENT_TIMESTAMP
                FROM scored
                ORDER BY position ASC, tier_score DESC
                """;

        int insertedCount = jdbcTemplate.update(insertSql);

        return StatsRebuildResponse.builder()
                .rebuiltCount(insertedCount)
                .message("챔피언 통계 재집계 완료: " + insertedCount + "개")
                .build();
    }

    public List<ChampionStatResponse> getChampionStats(String position) {
        return getChampionStats(position, null);
    }

    public List<ChampionStatResponse> getChampionStats(String position, String patch) {
        StringBuilder sql = new StringBuilder("""
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
            WHERE 1 = 1
            """);

        List<Object> params = new ArrayList<>();

        if (position != null && !position.isBlank()) {
            sql.append(" AND position = ? ");
            params.add(position.trim().toUpperCase());
        }

        if (patch != null && !patch.isBlank()) {
            sql.append(" AND patch = ? ");
            params.add(patch.trim());
        }

        sql.append(" ORDER BY tier_score DESC, games DESC ");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> ChampionStatResponse.builder()
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
                        .build(),
                params.toArray()
        );
    }
}