package com.game.backend.service;

import com.game.backend.dto.HomeMetaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeMetaService {

    private static final int SOLO_RANK_QUEUE_ID = 420;
    private static final int MIN_GAMES = 10;

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    public HomeMetaResponse getHomeMeta() {
        String dataDragonVersion = resolveDataDragonVersion();
        String latestPatch = resolveLatestPatch(dataDragonVersion);
        Map<String, ChampionNameMeta> championMetaMap = resolveChampionMetaMap(dataDragonVersion);

        MetaChampion hotChampion = findChampion(
                latestPatch,
                "pick_rate DESC, games DESC, win_rate DESC"
        );

        MetaChampion winRateChampion = findChampion(
                latestPatch,
                "win_rate DESC, games DESC, pick_rate DESC"
        );

        if (hotChampion == null) {
            hotChampion = fallbackChampion();
        }

        if (winRateChampion == null) {
            winRateChampion = hotChampion;
        }

        ChampionNameMeta hotMeta = championMetaMap.getOrDefault(
                hotChampion.championName(),
                new ChampionNameMeta(
                        hotChampion.championName(),
                        hotChampion.championName(),
                        String.valueOf(hotChampion.championId())
                )
        );

        long matchCount = countSql(
                "SELECT COUNT(*) FROM matches WHERE queue_id = ?",
                SOLO_RANK_QUEUE_ID
        );

        long participantCount = countSql(
                """
                SELECT COUNT(*)
                FROM match_participants mp
                JOIN matches m ON m.match_id = mp.match_id
                WHERE m.queue_id = ?
                """,
                SOLO_RANK_QUEUE_ID
        );

        long championStatCount = countSql(
                "SELECT COUNT(*) FROM champion_stats WHERE patch = ? AND queue_id = ? AND games >= ?",
                latestPatch,
                SOLO_RANK_QUEUE_ID,
                MIN_GAMES
        );

        long analyzedPickCount = countSql(
                "SELECT COALESCE(SUM(games), 0) FROM champion_stats WHERE patch = ? AND queue_id = ?",
                latestPatch,
                SOLO_RANK_QUEUE_ID
        );

        long totalChampionRows = countSql("SELECT COUNT(*) FROM champion_stats");

        int sampleHealth = clampPercent((int) Math.round(Math.min(100, analyzedPickCount / 100.0)));
        int metaDiversity = clampPercent((int) Math.round(Math.min(100, championStatCount * 100.0 / 80.0)));
        int representativeWinRate = clampPercent((int) Math.round(winRateChampion.winRate()));

        String hotNameKr = hotMeta.nameKr();
        String hotName = hotMeta.id();
        String championKey = hotMeta.key();
        String imageUrl = championImageUrl(dataDragonVersion, hotName);

        return HomeMetaResponse.builder()
                .hotChampion(
                        HomeMetaResponse.HotChampion.builder()
                                .name(hotName)
                                .nameKr(hotNameKr)
                                .championKey(championKey)
                                .position(positionLabel(hotChampion.position()))
                                .imageUrl(imageUrl)
                                .winRate(round1(hotChampion.winRate()))
                                .pickRate(round1(hotChampion.pickRate()))
                                .banRate(0.0)
                                .source("DB champion_stats · 솔로랭크")
                                .build()
                )
                .patchSummary(
                        HomeMetaResponse.PatchSummary.builder()
                                .version(latestPatch)
                                .summary("DB에 수집된 솔로랭크 데이터를 기준으로 최신 메타를 계산했습니다.")
                                .detail1("솔로랭크 수집 경기: " + formatNumber(matchCount) + "게임")
                                .detail2("분석 참가자: " + formatNumber(participantCount) + "명 · 분석 픽: " + formatNumber(analyzedPickCount) + "회")
                                .detail3("통계 대상 챔피언/포지션: " + formatNumber(championStatCount) + "개 · 전체 통계 Row: " + formatNumber(totalChampionRows) + "개")
                                .source("PostgreSQL DB")
                                .build()
                )
                .teamCompSummary(
                        HomeMetaResponse.TeamCompSummary.builder()
                                .apStatus(formatNumber(analyzedPickCount) + "픽")
                                .apRatio(sampleHealth)
                                .ccStatus(formatNumber(championStatCount) + "개")
                                .ccScore(metaDiversity)
                                .expectedWinRate(representativeWinRate)
                                .source("DB Meta Insight")
                                .build()
                )
                .aiFeedbackSummary(
                        HomeMetaResponse.AiFeedbackSummary.builder()
                                .feedback1("가장 많이 선택된 챔피언은 " + hotNameKr + "입니다. 픽률 " + round1(hotChampion.pickRate()) + "% / 승률 " + round1(hotChampion.winRate()) + "%")
                                .feedback2("최소 " + MIN_GAMES + "게임 이상 기준 최고 승률 픽은 " + championDisplayName(winRateChampion, championMetaMap) + "입니다. 승률 " + round1(winRateChampion.winRate()) + "%")
                                .feedback3("홈 화면 임의값을 제거하고, 최신 패치 " + latestPatch + "의 DB 통계 기반으로 표시합니다.")
                                .source("DB 기반 요약")
                                .build()
                )
                .build();
    }

    private String resolveDataDragonVersion() {
        try {
            List<String> versions = restTemplate.getForObject(
                    "https://ddragon.leagueoflegends.com/api/versions.json",
                    List.class
            );

            if (versions != null && !versions.isEmpty()) {
                return versions.get(0);
            }
        } catch (Exception ignored) {
        }

        return "16.10.1";
    }

    private String resolveLatestPatch(String dataDragonVersion) {
        String sql = """
                SELECT patch
                FROM champion_stats
                WHERE patch IS NOT NULL
                  AND patch <> ''
                GROUP BY patch
                ORDER BY split_part(patch, '.', 1)::int DESC,
                         split_part(patch, '.', 2)::int DESC,
                         COALESCE(SUM(games), 0) DESC
                LIMIT 1
                """;

        List<String> patches = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("patch")
        );

        if (!patches.isEmpty()) {
            return patches.get(0);
        }

        String[] parts = dataDragonVersion.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }

        return dataDragonVersion;
    }

    private MetaChampion findChampion(String patch, String orderBy) {
        String sql = """
                SELECT
                    champion_id,
                    champion_name,
                    position,
                    games,
                    wins,
                    win_rate,
                    pick_rate,
                    tier_score,
                    tier
                FROM champion_stats
                WHERE patch = ?
                  AND queue_id = ?
                  AND games >= ?
                ORDER BY %s
                LIMIT 1
                """.formatted(orderBy);

        List<MetaChampion> result = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new MetaChampion(
                        rs.getInt("champion_id"),
                        rs.getString("champion_name"),
                        rs.getString("position"),
                        rs.getInt("games"),
                        rs.getInt("wins"),
                        rs.getDouble("win_rate"),
                        rs.getDouble("pick_rate"),
                        rs.getDouble("tier_score"),
                        rs.getString("tier")
                ),
                patch,
                SOLO_RANK_QUEUE_ID,
                MIN_GAMES
        );

        return result.isEmpty() ? null : result.get(0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ChampionNameMeta> resolveChampionMetaMap(String version) {
        Map<String, ChampionNameMeta> metaMap = new HashMap<>();

        try {
            String championUrl = "https://ddragon.leagueoflegends.com/cdn/"
                    + version
                    + "/data/ko_KR/champion.json";

            Map<String, Object> championResponse = restTemplate.getForObject(championUrl, Map.class);

            if (championResponse == null || !(championResponse.get("data") instanceof Map<?, ?> rawData)) {
                return metaMap;
            }

            for (Object value : rawData.values()) {
                if (!(value instanceof Map<?, ?> rawChampion)) {
                    continue;
                }

                Map<String, Object> champion = (Map<String, Object>) rawChampion;
                String id = stringValue(champion.get("id"), "");
                String nameKr = stringValue(champion.get("name"), id);
                String key = stringValue(champion.get("key"), "");

                if (!id.isBlank()) {
                    metaMap.put(id, new ChampionNameMeta(id, nameKr, key));
                }
            }
        } catch (Exception ignored) {
        }

        return metaMap;
    }

    private long countSql(String sql, Object... args) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
            return value == null ? 0 : value;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private MetaChampion fallbackChampion() {
        return new MetaChampion(266, "Aatrox", "TOP", 0, 0, 0.0, 0.0, 0.0, "-");
    }

    private String championDisplayName(MetaChampion champion, Map<String, ChampionNameMeta> championMetaMap) {
        ChampionNameMeta meta = championMetaMap.get(champion.championName());
        return meta == null ? champion.championName() : meta.nameKr();
    }

    private String championImageUrl(String version, String championName) {
        return "https://ddragon.leagueoflegends.com/cdn/"
                + version
                + "/img/champion/"
                + championName
                + ".png";
    }

    private String positionLabel(String position) {
        if (position == null || position.isBlank()) {
            return "-";
        }

        return switch (position) {
            case "TOP" -> "탑";
            case "JUNGLE" -> "정글";
            case "MIDDLE", "MID" -> "미드";
            case "BOTTOM", "ADC" -> "원딜";
            case "UTILITY", "SUPPORT" -> "서포터";
            default -> position;
        };
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String formatNumber(long value) {
        return String.format("%,d", value);
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private record MetaChampion(
            Integer championId,
            String championName,
            String position,
            Integer games,
            Integer wins,
            Double winRate,
            Double pickRate,
            Double tierScore,
            String tier
    ) {
    }

    private record ChampionNameMeta(
            String id,
            String nameKr,
            String key
    ) {
    }
}
