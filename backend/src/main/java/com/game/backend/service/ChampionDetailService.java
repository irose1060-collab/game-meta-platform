package com.game.backend.service;

import com.game.backend.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChampionDetailService {

    private final JdbcTemplate jdbcTemplate;

    public ChampionAnalyticsDetailResponse getChampionDetail(int championId, String position, String patch) {
        String cleanPosition = position == null ? "" : position.trim().toUpperCase();
        String cleanPatch = patch == null ? "" : patch.trim();

        ChampionStatResponse basic = findBasicStat(championId, cleanPosition, cleanPatch);

        return ChampionAnalyticsDetailResponse.builder()
                .basic(basic)
                .spells(findSpellStats(championId, cleanPosition, basic.getPatch()))
                .items(findItemStats(championId, cleanPosition, basic.getPatch()))
                .hardCounters(findCounterStats(championId, cleanPosition, basic.getPatch(), true))
                .easyMatchups(findCounterStats(championId, cleanPosition, basic.getPatch(), false))
                .build();
    }

    private ChampionStatResponse findBasicStat(int championId, String position, String patch) {
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
                WHERE champion_id = ?
                  AND position = ?
                """);

        if (patch != null && !patch.isBlank()) {
            sql.append(" AND patch = ? ");
            sql.append(" ORDER BY games DESC LIMIT 1 ");

            List<ChampionStatResponse> result = jdbcTemplate.query(
                    sql.toString(),
                    (rs, rowNum) -> mapChampionStat(rs),
                    championId,
                    position,
                    patch
            );

            if (result.isEmpty()) {
                throw new IllegalArgumentException("챔피언 통계를 찾을 수 없습니다.");
            }

            return result.get(0);
        }

        sql.append("""
                ORDER BY
                    NULLIF(SPLIT_PART(patch, '.', 1), '')::int DESC,
                    NULLIF(SPLIT_PART(patch, '.', 2), '')::int DESC,
                    games DESC
                LIMIT 1
                """);

        List<ChampionStatResponse> result = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> mapChampionStat(rs),
                championId,
                position
        );

        if (result.isEmpty()) {
            throw new IllegalArgumentException("챔피언 통계를 찾을 수 없습니다.");
        }

        return result.get(0);
    }

    private List<SpellStatResponse> findSpellStats(int championId, String position, String patch) {
        String sql = """
                WITH base AS (
                    SELECT
                        LEAST(COALESCE(mp.summoner1_id, 0), COALESCE(mp.summoner2_id, 0)) AS spell1_id,
                        GREATEST(COALESCE(mp.summoner1_id, 0), COALESCE(mp.summoner2_id, 0)) AS spell2_id,
                        mp.win
                    FROM match_participants mp
                    JOIN matches m ON m.match_id = mp.match_id
                    WHERE mp.champion_id = ?
                      AND mp.team_position = ?
                      AND CONCAT(SPLIT_PART(m.game_version, '.', 1), '.', SPLIT_PART(m.game_version, '.', 2)) = ?
                      AND COALESCE(mp.summoner1_id, 0) > 0
                      AND COALESCE(mp.summoner2_id, 0) > 0
                )
                SELECT
                    spell1_id,
                    spell2_id,
                    COUNT(*) AS games,
                    SUM(CASE WHEN win = true THEN 1 ELSE 0 END) AS wins,
                    ROUND(
                        SUM(CASE WHEN win = true THEN 1 ELSE 0 END)::numeric * 100
                        / NULLIF(COUNT(*)::numeric, 0),
                        2
                    ) AS win_rate
                FROM base
                GROUP BY spell1_id, spell2_id
                ORDER BY games DESC, win_rate DESC
                LIMIT 5
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> SpellStatResponse.builder()
                        .spell1Id(rs.getInt("spell1_id"))
                        .spell2Id(rs.getInt("spell2_id"))
                        .games(rs.getInt("games"))
                        .wins(rs.getInt("wins"))
                        .winRate(rs.getDouble("win_rate"))
                        .build(),
                championId,
                position,
                patch
        );
    }

    private List<ItemStatResponse> findItemStats(int championId, String position, String patch) {
        String sql = """
            WITH item_rows AS (
                SELECT item.item_id, mp.win
                FROM match_participants mp
                JOIN matches m ON m.match_id = mp.match_id
                CROSS JOIN LATERAL (
                    VALUES
                        (mp.item0),
                        (mp.item1),
                        (mp.item2),
                        (mp.item3),
                        (mp.item4),
                        (mp.item5)
                ) AS item(item_id)
                WHERE mp.champion_id = ?
                  AND mp.team_position = ?
                  AND CONCAT(SPLIT_PART(m.game_version, '.', 1), '.', SPLIT_PART(m.game_version, '.', 2)) = ?
                  AND item.item_id IS NOT NULL
                  AND item.item_id <> 0

                  -- 신발 제외
                  AND item.item_id NOT IN (
                      1001, 3006, 3009, 3020, 3047, 3111, 3117, 3158, 3173, 2422
                  )

                  -- 시작 아이템 / 도란템 / 여눈 / 부패물약 등 제외
                  AND item.item_id NOT IN (
                      1054, 1055, 1056, 1082, 1083, 2031, 2033
                  )

                  -- 와드 / 장신구 / 제어 와드 / 소모품 제외
                  AND item.item_id NOT IN (
                      2003, 2010, 2055, 2138, 2139, 2140,
                      3330, 3340, 3363, 3364, 3513
                  )

                  -- 정글 시작 아이템 제외
                  AND item.item_id NOT IN (
                      1101, 1102, 1103
                  )

                  -- 서포터 퀘스트 계열 아이템 제외
                  AND item.item_id NOT IN (
                      3850, 3851, 3853, 3854,
                      3855, 3857, 3858,
                      3859, 3860, 3862,
                      3863, 3864, 3865,
                      3866, 3867, 3869,
                      3870, 3871, 3876, 3877
                  )
            ),
            agg AS (
                SELECT
                    item_id,
                    COUNT(*) AS games,
                    SUM(CASE WHEN win = true THEN 1 ELSE 0 END) AS wins,
                    ROUND(
                        SUM(CASE WHEN win = true THEN 1 ELSE 0 END)::numeric * 100
                        / NULLIF(COUNT(*)::numeric, 0),
                        2
                    ) AS win_rate
                FROM item_rows
                GROUP BY item_id
            )
            SELECT
                item_id,
                games,
                wins,
                win_rate
            FROM agg
            WHERE games >= 2
            ORDER BY games DESC, win_rate DESC
            LIMIT 8
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> ItemStatResponse.builder()
                        .itemId(rs.getInt("item_id"))
                        .games(rs.getInt("games"))
                        .wins(rs.getInt("wins"))
                        .winRate(rs.getDouble("win_rate"))
                        .build(),
                championId,
                position,
                patch
        );
    }

    private List<CounterStatResponse> findCounterStats(
            int championId,
            String position,
            String patch,
            boolean hardCounters
    ) {
        String orderBy = hardCounters
                ? " ORDER BY win_rate ASC, games DESC "
                : " ORDER BY win_rate DESC, games DESC ";

        String sql = """
            WITH matchup AS (
                SELECT
                    enemy.champion_id AS enemy_champion_id,
                    enemy.champion_name AS enemy_champion_name,
                    me.win
                FROM match_participants me
                JOIN matches m ON m.match_id = me.match_id
                JOIN match_participants enemy
                  ON enemy.match_id = me.match_id
                 AND enemy.team_position = me.team_position
                 AND enemy.team_id <> me.team_id
                WHERE me.champion_id = ?
                  AND me.team_position = ?
                  AND CONCAT(SPLIT_PART(m.game_version, '.', 1), '.', SPLIT_PART(m.game_version, '.', 2)) = ?
                  AND enemy.champion_id IS NOT NULL
                  AND enemy.champion_name IS NOT NULL
            ),
            agg AS (
                SELECT
                    enemy_champion_id,
                    enemy_champion_name,
                    COUNT(*) AS games,
                    SUM(CASE WHEN win = true THEN 1 ELSE 0 END) AS wins,
                    ROUND(
                        SUM(CASE WHEN win = true THEN 1 ELSE 0 END)::numeric * 100
                        / NULLIF(COUNT(*)::numeric, 0),
                        2
                    ) AS win_rate
                FROM matchup
                GROUP BY enemy_champion_id, enemy_champion_name
                HAVING COUNT(*) >= 1
            )
            SELECT
                enemy_champion_id,
                enemy_champion_name,
                games,
                wins,
                win_rate
            FROM agg
            """ + orderBy + """
            LIMIT 8
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> CounterStatResponse.builder()
                        .enemyChampionId(rs.getInt("enemy_champion_id"))
                        .enemyChampionName(rs.getString("enemy_champion_name"))
                        .games(rs.getInt("games"))
                        .wins(rs.getInt("wins"))
                        .winRate(rs.getDouble("win_rate"))
                        .build(),
                championId,
                position,
                patch
        );
    }
    private ChampionStatResponse mapChampionStat(java.sql.ResultSet rs) throws java.sql.SQLException {
        return ChampionStatResponse.builder()
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