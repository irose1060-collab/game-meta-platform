package com.game.backend.service;

import com.game.backend.dto.AssetDto;
import com.game.backend.dto.MatchParticipantResponse;
import com.game.backend.dto.MatchSearchResponse;
import com.game.backend.dto.MatchSummaryResponse;
import com.game.backend.dto.MatchTeamSummaryResponse;
import com.game.backend.dto.RiotAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchSearchService {

    private final RiotService riotService;
    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    @Value("${riot.account.base-url}")
    private String riotBaseUrl;

    @Value("${riot.platform.base-url:https://kr.api.riotgames.com}")
    private String riotPlatformBaseUrl;

    public MatchSearchResponse searchMatches(String gameName, String tagLine, int count) {
        int safeCount = Math.min(Math.max(count, 1), 10);

        RiotAccountResponse account = riotService.getAccountByRiotId(gameName, tagLine);
        String puuid = account.getPuuid();

        List<String> matchIds = getRecentMatchIds(puuid, safeCount);
        String latestVersion = getLatestVersion();

        Map<Integer, AssetDto> runeMap = getRuneMap(latestVersion);
        Map<String, ChampionMeta> championKeyMap = getChampionKeyMap(latestVersion);
        Map<String, String> rankCache = new HashMap<>();

        List<MatchSummaryResponse> matches = new ArrayList<>();

        for (String matchId : matchIds) {
            MatchSummaryResponse match =
                    getMatchSummary(matchId, puuid, latestVersion, runeMap, championKeyMap, rankCache);

            if (match != null) {
                matches.add(match);
            }
        }

        int wins = (int) matches.stream()
                .filter(match -> Boolean.TRUE.equals(match.getWin()))
                .count();

        int losses = matches.size() - wins;

        double winRate = matches.isEmpty()
                ? 0.0
                : round1((wins * 100.0) / matches.size());

        double averageKda = matches.isEmpty()
                ? 0.0
                : round2(
                matches.stream()
                .mapToDouble(MatchSummaryResponse::getKda)
                .average()
                .orElse(0.0)
        );

        return MatchSearchResponse.builder()
                .gameName(account.getGameName())
                .tagLine(account.getTagLine())
                .puuid(account.getPuuid())
                .totalMatches(matches.size())
                .wins(wins)
                .losses(losses)
                .winRate(winRate)
                .averageKda(averageKda)
                .matches(matches)
                .build();
    }

    private List<String> getRecentMatchIds(String puuid, int count) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(riotBaseUrl)
                .pathSegment("lol", "match", "v5", "matches", "by-puuid", puuid, "ids")
                .queryParam("start", 0)
                .queryParam("count", count)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<String>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<String>>() {
                }
        );

        return response.getBody() != null ? response.getBody() : List.of();
    }

    @SuppressWarnings("unchecked")
    private MatchSummaryResponse getMatchSummary(
            String matchId,
            String searchedPuuid,
            String latestVersion,
            Map<Integer, AssetDto> runeMap,
            Map<String, ChampionMeta> championKeyMap,
            Map<String, String> rankCache
    ) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(riotBaseUrl)
                .pathSegment("lol", "match", "v5", "matches", matchId)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        );

        Map<String, Object> matchData = response.getBody();

        if (matchData == null || !(matchData.get("info") instanceof Map<?, ?> rawInfo)) {
            return null;
        }

        Map<String, Object> info = (Map<String, Object>) rawInfo;

        if (!(info.get("participants") instanceof List<?> participants)) {
            return null;
        }

        Integer gameDurationSeconds = intValue(info.get("gameDuration"), 0);
        Long gameStartTimestamp = longValue(info.get("gameStartTimestamp"), 0L);
        Integer queueId = intValue(info.get("queueId"), 0);

        Map<String, Object> targetParticipant = null;

        int blueTeamTotalKills = 0;
        int redTeamTotalKills = 0;
        int blueTeamTotalGold = 0;
        int redTeamTotalGold = 0;
        int blueTeamTotalDamage = 0;
        int redTeamTotalDamage = 0;
        int maxDamage = 0;

        List<Map<String, Object>> participantMaps = new ArrayList<>();

        for (Object participantObject : participants) {
            if (participantObject instanceof Map<?, ?> rawParticipant) {
                Map<String, Object> participant = (Map<String, Object>) rawParticipant;
                participantMaps.add(participant);

                if (searchedPuuid.equals(String.valueOf(participant.get("puuid")))) {
                    targetParticipant = participant;
                }

                int teamId = intValue(participant.get("teamId"), 0);
                int kills = intValue(participant.get("kills"), 0);
                int gold = intValue(participant.get("goldEarned"), 0);
                int damage = intValue(participant.get("totalDamageDealtToChampions"), 0);

                maxDamage = Math.max(maxDamage, damage);

                if (teamId == 100) {
                    blueTeamTotalKills += kills;
                    blueTeamTotalGold += gold;
                    blueTeamTotalDamage += damage;
                } else if (teamId == 200) {
                    redTeamTotalKills += kills;
                    redTeamTotalGold += gold;
                    redTeamTotalDamage += damage;
                }
            }
        }

        if (targetParticipant == null) {
            return null;
        }

        Map<String, Double> opScoreMap = new HashMap<>();

        for (Map<String, Object> participant : participantMaps) {
            int teamId = intValue(participant.get("teamId"), 0);
            int teamKills = teamId == 100 ? blueTeamTotalKills : redTeamTotalKills;

            double opScore = calculateOpScore(participant, teamKills, maxDamage, gameDurationSeconds);
            opScoreMap.put(participantKey(participant), opScore);
        }

        List<Map.Entry<String, Double>> rankedScores = opScoreMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();

        Map<String, Integer> opRankMap = new HashMap<>();

        for (int i = 0; i < rankedScores.size(); i++) {
            opRankMap.put(rankedScores.get(i).getKey(), i + 1);
        }

        List<MatchParticipantResponse> blueTeam = new ArrayList<>();
        List<MatchParticipantResponse> redTeam = new ArrayList<>();

        for (Map<String, Object> participant : participantMaps) {
            int teamId = intValue(participant.get("teamId"), 0);
            int teamKills = teamId == 100 ? blueTeamTotalKills : redTeamTotalKills;
            int teamDamage = teamId == 100 ? blueTeamTotalDamage : redTeamTotalDamage;

            MatchParticipantResponse participantResponse = toParticipantResponse(
                    participant,
                    latestVersion,
                    runeMap,
                    rankCache,
                    queueId,
                    teamKills,
                    teamDamage,
                    gameDurationSeconds,
                    opScoreMap,
                    opRankMap
            );

            if (teamId == 100) {
                blueTeam.add(participantResponse);
            } else if (teamId == 200) {
                redTeam.add(participantResponse);
            }
        }

        blueTeam.sort(Comparator.comparingInt(participant -> positionOrder(participant.getTeamPosition())));
        redTeam.sort(Comparator.comparingInt(participant -> positionOrder(participant.getTeamPosition())));

        MatchTeamSummaryResponse blueTeamSummary = getTeamSummary(
                info,
                100,
                blueTeamTotalKills,
                blueTeamTotalGold,
                latestVersion,
                championKeyMap
        );

        MatchTeamSummaryResponse redTeamSummary = getTeamSummary(
                info,
                200,
                redTeamTotalKills,
                redTeamTotalGold,
                latestVersion,
                championKeyMap
        );

        MatchParticipantResponse targetResponse = toParticipantResponse(
                targetParticipant,
                latestVersion,
                runeMap,
                rankCache,
                queueId,
                intValue(targetParticipant.get("teamId"), 0) == 100 ? blueTeamTotalKills : redTeamTotalKills,
                intValue(targetParticipant.get("teamId"), 0) == 100 ? blueTeamTotalDamage : redTeamTotalDamage,
                gameDurationSeconds,
                opScoreMap,
                opRankMap
        );

        return MatchSummaryResponse.builder()
                .matchId(matchId)
                .gameStartTimestamp(gameStartTimestamp)
                .playedAtText(formatPlayedAt(gameStartTimestamp))
                .championName(targetResponse.getChampionName())
                .championImageUrl(targetResponse.getChampionImageUrl())
                .win(targetResponse.getWin())
                .resultText(Boolean.TRUE.equals(targetResponse.getWin()) ? "승리" : "패배")
                .kills(targetResponse.getKills())
                .deaths(targetResponse.getDeaths())
                .assists(targetResponse.getAssists())
                .kda(targetResponse.getKda())
                .killParticipation(targetResponse.getKillParticipation())
                .position(targetResponse.getTeamPosition())
                .gameMode(stringValue(info.get("gameMode"), "-"))
                .queueType(queueType(queueId))
                .queueId(queueId)
                .gameDurationSeconds(gameDurationSeconds)
                .gameDurationText(formatDuration(gameDurationSeconds))
                .totalCs(targetResponse.getTotalCs())
                .csPerMinute(targetResponse.getCsPerMinute())
                .goldEarned(targetResponse.getGoldEarned())
                .totalDamageDealtToChampions(targetResponse.getTotalDamageDealtToChampions())
                .totalDamageTaken(targetResponse.getTotalDamageTaken())
                .visionScore(targetResponse.getVisionScore())
                .wardsPlaced(targetResponse.getWardsPlaced())
                .wardsKilled(targetResponse.getWardsKilled())
                .controlWardsPlaced(targetResponse.getControlWardsPlaced())
                .opScore(targetResponse.getOpScore())
                .opScoreRank(targetResponse.getOpScoreRank())
                .opScoreBadge(targetResponse.getOpScoreBadge())
                .rankTier(targetResponse.getRankTier())
                .items(targetResponse.getItems())
                .summonerSpells(targetResponse.getSummonerSpells())
                .runes(targetResponse.getRunes())
                .blueTeam(blueTeam)
                .redTeam(redTeam)
                .blueTeamTotalKills(blueTeamTotalKills)
                .redTeamTotalKills(redTeamTotalKills)
                .blueTeamTotalGold(blueTeamTotalGold)
                .redTeamTotalGold(redTeamTotalGold)
                .maxDamage(maxDamage)
                .blueTeamSummary(blueTeamSummary)
                .redTeamSummary(redTeamSummary)
                .build();
    }

    private MatchParticipantResponse toParticipantResponse(
            Map<String, Object> participant,
            String latestVersion,
            Map<Integer, AssetDto> runeMap,
            Map<String, String> rankCache,
            Integer queueId,
            Integer teamKills,
            Integer teamDamage,
            Integer gameDurationSeconds,
            Map<String, Double> opScoreMap,
            Map<String, Integer> opRankMap
    ) {
        String championName = stringValue(participant.get("championName"), "Unknown");

        Integer kills = intValue(participant.get("kills"), 0);
        Integer deaths = intValue(participant.get("deaths"), 0);
        Integer assists = intValue(participant.get("assists"), 0);
        Integer totalMinionsKilled = intValue(participant.get("totalMinionsKilled"), 0);
        Integer neutralMinionsKilled = intValue(participant.get("neutralMinionsKilled"), 0);
        Integer totalCs = totalMinionsKilled + neutralMinionsKilled;
        Integer damage = intValue(participant.get("totalDamageDealtToChampions"), 0);

        Double kda = calculateKda(kills, deaths, assists);

        Integer killParticipation = teamKills == null || teamKills == 0
                ? 0
                : (int) Math.round(((kills + assists) * 100.0) / teamKills);

        Double damageShare = teamDamage == null || teamDamage == 0
                ? 0.0
                : round1((damage * 100.0) / teamDamage);

        Double csPerMinute = gameDurationSeconds == null || gameDurationSeconds == 0
                ? 0.0
                : round1(totalCs / (gameDurationSeconds / 60.0));

        String key = participantKey(participant);
        Double opScore = round1(opScoreMap.getOrDefault(key, 0.0));
        Integer opRank = opRankMap.getOrDefault(key, 10);

        return MatchParticipantResponse.builder()
                .puuid(stringValue(participant.get("puuid"), ""))
                .summonerId(stringValue(participant.get("summonerId"), ""))
                .summonerName(buildRiotName(participant))
                .riotGameName(stringValue(participant.get("riotIdGameName"), ""))
                .riotTagLine(stringValue(participant.get("riotIdTagline"), ""))
                .teamId(intValue(participant.get("teamId"), 0))
                .teamPosition(getPosition(participant))
                .championName(championName)
                .championImageUrl(championImageUrl(latestVersion, championName))
                .championLevel(intValue(participant.get("champLevel"), 0))
                .win(booleanValue(participant.get("win"), false))
                .kills(kills)
                .deaths(deaths)
                .assists(assists)
                .kda(kda)
                .killParticipation(killParticipation)
                .damageShare(damageShare)
                .totalCs(totalCs)
                .csPerMinute(csPerMinute)
                .goldEarned(intValue(participant.get("goldEarned"), 0))
                .totalDamageDealtToChampions(damage)
                .totalDamageTaken(intValue(participant.get("totalDamageTaken"), 0))
                .visionScore(intValue(participant.get("visionScore"), 0))
                .wardsPlaced(intValue(participant.get("wardsPlaced"), 0))
                .wardsKilled(intValue(participant.get("wardsKilled"), 0))
                .controlWardsPlaced(intValue(participant.get("detectorWardsPlaced"), 0))
                .opScore(opScore)
                .opScoreRank(opRank)
                .opScoreBadge(opScoreBadge(opRank, booleanValue(participant.get("win"), false)))
                .rankTier(getRankTier(stringValue(participant.get("summonerId"), ""), queueId, rankCache))
                .items(makeItems(participant, latestVersion))
                .summonerSpells(makeSummonerSpells(participant, latestVersion))
                .runes(makeRunes(participant, runeMap))
                .build();
    }

    @SuppressWarnings("unchecked")
    private MatchTeamSummaryResponse getTeamSummary(
            Map<String, Object> info,
            Integer teamId,
            Integer totalKills,
            Integer totalGold,
            String version,
            Map<String, ChampionMeta> championKeyMap
    ) {
        Boolean win = false;
        Integer baronKills = 0;
        Integer dragonKills = 0;
        Integer riftHeraldKills = 0;
        Integer hordeKills = 0;
        Integer towerKills = 0;
        Integer inhibitorKills = 0;
        List<AssetDto> bans = new ArrayList<>();

        Object teamsObject = info.get("teams");

        if (teamsObject instanceof List<?> teams) {
            for (Object teamObject : teams) {
                if (teamObject instanceof Map<?, ?> rawTeam) {
                    Map<String, Object> team = (Map<String, Object>) rawTeam;

                    if (!Objects.equals(intValue(team.get("teamId"), 0), teamId)) {
                        continue;
                    }

                    win = booleanValue(team.get("win"), false);

                    if (team.get("objectives") instanceof Map<?, ?> rawObjectives) {
                        Map<String, Object> objectives = (Map<String, Object>) rawObjectives;

                        baronKills = objectiveKills(objectives, "baron");
                        dragonKills = objectiveKills(objectives, "dragon");
                        riftHeraldKills = objectiveKills(objectives, "riftHerald");
                        hordeKills = objectiveKills(objectives, "horde");
                        towerKills = objectiveKills(objectives, "tower");
                        inhibitorKills = objectiveKills(objectives, "inhibitor");
                    }

                    if (team.get("bans") instanceof List<?> rawBans) {
                        for (Object banObject : rawBans) {
                            if (banObject instanceof Map<?, ?> rawBan) {
                                Map<String, Object> ban = (Map<String, Object>) rawBan;

                                Integer championId = intValue(ban.get("championId"), -1);

                                if (championId == null || championId <= 0) {
                                    continue;
                                }

                                ChampionMeta meta = championKeyMap.get(String.valueOf(championId));

                                if (meta == null) {
                                    continue;
                                }

                                bans.add(
                                        AssetDto.builder()
                                                .id(String.valueOf(championId))
                                                .name(meta.name())
                                                .description("밴 챔피언")
                                                .imageUrl(championImageUrl(version, meta.id()))
                                                .build()
                                );
                            }
                        }
                    }
                }
            }
        }

        return MatchTeamSummaryResponse.builder()
                .teamId(teamId)
                .win(win)
                .totalKills(totalKills)
                .totalGold(totalGold)
                .baronKills(baronKills)
                .dragonKills(dragonKills)
                .riftHeraldKills(riftHeraldKills)
                .hordeKills(hordeKills)
                .towerKills(towerKills)
                .inhibitorKills(inhibitorKills)
                .bans(bans)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Integer objectiveKills(Map<String, Object> objectives, String key) {
        Object objective = objectives.get(key);

        if (objective instanceof Map<?, ?> rawObjective) {
            Map<String, Object> objectiveMap = (Map<String, Object>) rawObjective;
            return intValue(objectiveMap.get("kills"), 0);
        }

        return 0;
    }

    private Double calculateOpScore(
            Map<String, Object> participant,
            Integer teamKills,
            Integer maxDamage,
            Integer gameDurationSeconds
    ) {
        Integer kills = intValue(participant.get("kills"), 0);
        Integer deaths = intValue(participant.get("deaths"), 0);
        Integer assists = intValue(participant.get("assists"), 0);
        Integer damage = intValue(participant.get("totalDamageDealtToChampions"), 0);
        Integer visionScore = intValue(participant.get("visionScore"), 0);

        Integer totalCs = intValue(participant.get("totalMinionsKilled"), 0)
                + intValue(participant.get("neutralMinionsKilled"), 0);

        Double kda = calculateKda(kills, deaths, assists);

        double killParticipationScore = teamKills == null || teamKills == 0
                ? 0
                : Math.min(20, ((kills + assists) * 100.0 / teamKills) * 0.2);

        double damageScore = maxDamage == null || maxDamage == 0
                ? 0
                : Math.min(20, (damage * 100.0 / maxDamage) * 0.2);

        double kdaScore = Math.min(25, kda * 3.0);

        double csPerMinute = gameDurationSeconds == null || gameDurationSeconds == 0
                ? 0
                : totalCs / (gameDurationSeconds / 60.0);

        double csScore = Math.min(15, csPerMinute * 1.5);
        double visionScorePart = Math.min(10, visionScore * 0.2);
        double winBonus = booleanValue(participant.get("win"), false) ? 5 : 0;

        return round1(25 + killParticipationScore + damageScore + kdaScore + csScore + visionScorePart + winBonus);
    }

    private String opScoreBadge(Integer rank, Boolean win) {
        if (rank == null) {
            return "-";
        }

        if (rank == 1) {
            return Boolean.TRUE.equals(win) ? "MVP" : "ACE";
        }

        return rank + "th";
    }

    private String participantKey(Map<String, Object> participant) {
        return stringValue(participant.get("puuid"), "")
                + "_"
                + stringValue(participant.get("championName"), "")
                + "_"
                + stringValue(participant.get("teamId"), "");
    }

    private String buildRiotName(Map<String, Object> participant) {
        String riotGameName = stringValue(participant.get("riotIdGameName"), "");
        String riotTagLine = stringValue(participant.get("riotIdTagline"), "");

        if (!riotGameName.isBlank()) {
            return riotTagLine.isBlank()
                    ? riotGameName
                    : riotGameName + "#" + riotTagLine;
        }

        String summonerName = stringValue(participant.get("summonerName"), "");

        if (!summonerName.isBlank()) {
            return summonerName;
        }

        return "Unknown";
    }

    private List<AssetDto> makeItems(Map<String, Object> participant, String version) {
        List<AssetDto> items = new ArrayList<>();

        for (int i = 0; i <= 6; i++) {
            int itemId = intValue(participant.get("item" + i), 0);

            if (itemId == 0) {
                continue;
            }

            items.add(
                    AssetDto.builder()
                            .id(String.valueOf(itemId))
                            .name("아이템 " + itemId)
                            .description("장착 아이템")
                            .imageUrl(itemImageUrl(version, itemId))
                            .build()
            );
        }

        return items;
    }

    private List<AssetDto> makeSummonerSpells(Map<String, Object> participant, String version) {
        int spell1Id = intValue(participant.get("summoner1Id"), 0);
        int spell2Id = intValue(participant.get("summoner2Id"), 0);

        List<AssetDto> spells = new ArrayList<>();

        if (spell1Id != 0) {
            spells.add(makeSpellAsset(version, spell1Id));
        }

        if (spell2Id != 0) {
            spells.add(makeSpellAsset(version, spell2Id));
        }

        return spells;
    }

    @SuppressWarnings("unchecked")
    private List<AssetDto> makeRunes(Map<String, Object> participant, Map<Integer, AssetDto> runeMap) {
        List<AssetDto> runes = new ArrayList<>();

        Object perksObject = participant.get("perks");

        if (!(perksObject instanceof Map<?, ?> rawPerks)) {
            return runes;
        }

        Map<String, Object> perks = (Map<String, Object>) rawPerks;

        Object stylesObject = perks.get("styles");

        if (!(stylesObject instanceof List<?> styles)) {
            return runes;
        }

        for (Object styleObject : styles) {
            if (!(styleObject instanceof Map<?, ?> rawStyle)) {
                continue;
            }

            Map<String, Object> style = (Map<String, Object>) rawStyle;

            Object selectionsObject = style.get("selections");

            if (!(selectionsObject instanceof List<?> selections)) {
                continue;
            }

            for (Object selectionObject : selections) {
                if (!(selectionObject instanceof Map<?, ?> rawSelection)) {
                    continue;
                }

                Map<String, Object> selection = (Map<String, Object>) rawSelection;
                Integer perkId = intValue(selection.get("perk"), 0);

                if (perkId == 0) {
                    continue;
                }

                AssetDto rune = runeMap.get(perkId);

                if (rune != null) {
                    runes.add(rune);
                }

                if (runes.size() >= 6) {
                    return runes;
                }
            }
        }

        return runes;
    }

    private AssetDto makeSpellAsset(String version, int spellId) {
        SpellInfo spellInfo = spellMap().getOrDefault(
                spellId,
                new SpellInfo(String.valueOf(spellId), "SummonerFlash.png")
        );

        return AssetDto.builder()
                .id(String.valueOf(spellId))
                .name(spellInfo.name)
                .description("소환사 주문")
                .imageUrl(
                        "https://ddragon.leagueoflegends.com/cdn/"
                                + version
                                + "/img/spell/"
                                + spellInfo.imageFull
                )
                .build();
    }

    private Map<Integer, SpellInfo> spellMap() {
        Map<Integer, SpellInfo> map = new HashMap<>();

        map.put(1, new SpellInfo("정화", "SummonerBoost.png"));
        map.put(3, new SpellInfo("탈진", "SummonerExhaust.png"));
        map.put(4, new SpellInfo("점멸", "SummonerFlash.png"));
        map.put(6, new SpellInfo("유체화", "SummonerHaste.png"));
        map.put(7, new SpellInfo("회복", "SummonerHeal.png"));
        map.put(11, new SpellInfo("강타", "SummonerSmite.png"));
        map.put(12, new SpellInfo("순간이동", "SummonerTeleport.png"));
        map.put(13, new SpellInfo("총명", "SummonerMana.png"));
        map.put(14, new SpellInfo("점화", "SummonerDot.png"));
        map.put(21, new SpellInfo("방어막", "SummonerBarrier.png"));
        map.put(32, new SpellInfo("표식", "SummonerSnowball.png"));

        return map;
    }

    private String getRankTier(String summonerId, Integer queueId, Map<String, String> rankCache) {
        if (summonerId == null || summonerId.isBlank()) {
            return "Unranked";
        }

        if (rankCache.containsKey(summonerId)) {
            return rankCache.get(summonerId);
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(riotPlatformBaseUrl)
                    .pathSegment("lol", "league", "v4", "entries", "by-summoner", summonerId)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", riotApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }
            );

            List<Map<String, Object>> entries = response.getBody();

            if (entries == null || entries.isEmpty()) {
                rankCache.put(summonerId, "Unranked");
                return "Unranked";
            }

            String preferredQueue = queueId != null && queueId == 440
                    ? "RANKED_FLEX_SR"
                    : "RANKED_SOLO_5x5";

            Map<String, Object> selected = entries.stream()
                    .filter(entry -> preferredQueue.equals(String.valueOf(entry.get("queueType"))))
                    .findFirst()
                    .orElse(entries.get(0));

            String tier = stringValue(selected.get("tier"), "Unranked");
            String rank = stringValue(selected.get("rank"), "");

            String rankTier = rank.isBlank() ? tier : tier + " " + rank;

            rankCache.put(summonerId, rankTier);
            return rankTier;
        } catch (Exception e) {
            rankCache.put(summonerId, "Unranked");
            return "Unranked";
        }
    }

    private String getPosition(Map<String, Object> participant) {
        String teamPosition = stringValue(participant.get("teamPosition"), "");

        if (!teamPosition.isBlank()) {
            return normalizePosition(teamPosition);
        }

        String individualPosition = stringValue(participant.get("individualPosition"), "");

        if (!individualPosition.isBlank()) {
            return normalizePosition(individualPosition);
        }

        return "-";
    }

    private String normalizePosition(String position) {
        return switch (position) {
            case "TOP" -> "TOP";
            case "JUNGLE" -> "JUNGLE";
            case "MIDDLE" -> "MID";
            case "BOTTOM" -> "ADC";
            case "UTILITY" -> "SUPPORT";
            default -> position;
        };
    }

    private int positionOrder(String position) {
        return switch (position) {
            case "TOP" -> 1;
            case "JUNGLE" -> 2;
            case "MID", "MIDDLE" -> 3;
            case "ADC", "BOTTOM" -> 4;
            case "SUPPORT", "UTILITY" -> 5;
            default -> 9;
        };
    }

    private String queueType(Integer queueId) {
        return switch (queueId) {
            case 420 -> "솔로랭크";
            case 440 -> "자유랭크";
            case 400 -> "일반 교차";
            case 430 -> "일반";
            case 450 -> "칼바람";
            case 700 -> "격전";
            default -> "기타";
        };
    }

    private String championImageUrl(String version, String championName) {
        return "https://ddragon.leagueoflegends.com/cdn/"
                + version
                + "/img/champion/"
                + championName
                + ".png";
    }

    private String itemImageUrl(String version, int itemId) {
        return "https://ddragon.leagueoflegends.com/cdn/"
                + version
                + "/img/item/"
                + itemId
                + ".png";
    }

    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainSeconds = seconds % 60;

        return minutes + "분 " + remainSeconds + "초";
    }

    private String formatPlayedAt(Long gameStartTimestamp) {
        if (gameStartTimestamp == null || gameStartTimestamp == 0) {
            return "-";
        }

        LocalDateTime playedAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(gameStartTimestamp),
                ZoneId.systemDefault()
        );

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(playedAt, now);

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) {
            return "방금 전";
        }

        if (minutes < 60) {
            return minutes + "분 전";
        }

        if (hours < 24) {
            return hours + "시간 전";
        }

        return days + "일 전";
    }

    private String getLatestVersion() {
        try {
            List<String> versions = restTemplate.getForObject(
                    "https://ddragon.leagueoflegends.com/api/versions.json",
                    List.class
            );

            return versions != null && !versions.isEmpty()
                    ? versions.get(0)
                    : "16.10.1";
        } catch (Exception e) {
            return "16.10.1";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, AssetDto> getRuneMap(String version) {
        Map<Integer, AssetDto> runeMap = new HashMap<>();

        try {
            String url = "https://ddragon.leagueoflegends.com/cdn/"
                    + version
                    + "/data/ko_KR/runesReforged.json";

            List<Map<String, Object>> trees = restTemplate.getForObject(url, List.class);

            if (trees == null) {
                return runeMap;
            }

            for (Map<String, Object> tree : trees) {
                Object slotsObject = tree.get("slots");

                if (!(slotsObject instanceof List<?> slots)) {
                    continue;
                }

                for (Object slotObject : slots) {
                    if (!(slotObject instanceof Map<?, ?> rawSlot)) {
                        continue;
                    }

                    Map<String, Object> slot = (Map<String, Object>) rawSlot;

                    Object runesObject = slot.get("runes");

                    if (!(runesObject instanceof List<?> runes)) {
                        continue;
                    }

                    for (Object runeObject : runes) {
                        if (!(runeObject instanceof Map<?, ?> rawRune)) {
                            continue;
                        }

                        Map<String, Object> rune = (Map<String, Object>) rawRune;

                        Integer id = intValue(rune.get("id"), 0);
                        String name = stringValue(rune.get("name"), "");
                        String icon = stringValue(rune.get("icon"), "");

                        if (id == 0 || icon.isBlank()) {
                            continue;
                        }

                        runeMap.put(
                                id,
                                AssetDto.builder()
                                        .id(String.valueOf(id))
                                        .name(name)
                                        .description("룬")
                                        .imageUrl("https://ddragon.leagueoflegends.com/cdn/img/" + icon)
                                        .build()
                        );
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return runeMap;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ChampionMeta> getChampionKeyMap(String version) {
        Map<String, ChampionMeta> championKeyMap = new HashMap<>();

        try {
            String url = "https://ddragon.leagueoflegends.com/cdn/"
                    + version
                    + "/data/ko_KR/champion.json";

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
                return championKeyMap;
            }

            for (Object value : data.values()) {
                if (!(value instanceof Map<?, ?> rawChampion)) {
                    continue;
                }

                Map<String, Object> champion = (Map<String, Object>) rawChampion;

                String key = stringValue(champion.get("key"), "");
                String id = stringValue(champion.get("id"), "");
                String name = stringValue(champion.get("name"), id);

                if (!key.isBlank() && !id.isBlank()) {
                    championKeyMap.put(key, new ChampionMeta(id, name));
                }
            }
        } catch (Exception ignored) {
        }

        return championKeyMap;
    }

    private Double calculateKda(Integer kills, Integer deaths, Integer assists) {
        if (deaths == 0) {
            return round2(kills + assists);
        }

        return round2((kills + assists) / (double) deaths);
    }

    private String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    private Integer intValue(Object value, Integer fallback) {
        try {
            return value != null ? Integer.parseInt(String.valueOf(value)) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private Long longValue(Object value, Long fallback) {
        try {
            return value != null ? Long.parseLong(String.valueOf(value)) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private Boolean booleanValue(Object value, Boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value));
        }

        return fallback;
    }

    private Double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record SpellInfo(String name, String imageFull) {
    }

    private record ChampionMeta(String id, String name) {
    }
}