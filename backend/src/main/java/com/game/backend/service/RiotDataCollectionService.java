package com.game.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.game.backend.dto.CollectionResultResponse;
import com.game.backend.dto.RiotAccountResponse;
import com.game.backend.entity.MatchEntity;
import com.game.backend.entity.MatchParticipant;
import com.game.backend.repository.MatchParticipantRepository;
import com.game.backend.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiotDataCollectionService {

    private final RiotService riotService;
    private final RestTemplate restTemplate;
    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Value("${riot.api.key}")
    private String riotApiKey;

    @Value("${riot.match.base-url:https://asia.api.riotgames.com}")
    private String matchBaseUrl;

    @Value("${riot.request-delay-ms:1300}")
    private long requestDelayMs;

    /**
     * Riot ID 기준 최근 랭크 매치를 수집한다.
     * queue=420은 솔로랭크 기준이다.
     */
    @Transactional
    public CollectionResultResponse collectRecentMatchesByRiotId(String gameName, String tagLine, int count) {
        int safeCount = Math.max(1, Math.min(count, 20));
        RiotAccountResponse account = riotService.getAccountByRiotId(gameName, tagLine);

        List<String> matchIds = fetchMatchIds(account.getPuuid(), safeCount, 420);

        int savedMatchCount = 0;
        int skippedExistingMatchCount = 0;
        int failedMatchCount = 0;
        int savedParticipantCount = 0;
        List<String> savedMatchIds = new ArrayList<>();
        List<String> failedMatchIds = new ArrayList<>();

        for (String matchId : matchIds) {
            try {
                if (matchRepository.existsByMatchId(matchId)) {
                    skippedExistingMatchCount++;
                    continue;
                }

                JsonNode matchDetail = fetchMatchDetail(matchId);
                int participantCount = saveMatchDetail(matchId, matchDetail);
                savedParticipantCount += participantCount;
                savedMatchCount++;
                savedMatchIds.add(matchId);
            } catch (Exception e) {
                failedMatchCount++;
                failedMatchIds.add(matchId + " :: " + e.getMessage());
            }
        }

        return CollectionResultResponse.builder()
                .gameName(account.getGameName())
                .tagLine(account.getTagLine())
                .puuid(account.getPuuid())
                .requestedCount(safeCount)
                .receivedMatchIdCount(matchIds.size())
                .savedMatchCount(savedMatchCount)
                .skippedExistingMatchCount(skippedExistingMatchCount)
                .failedMatchCount(failedMatchCount)
                .savedParticipantCount(savedParticipantCount)
                .savedMatchIds(savedMatchIds)
                .failedMatchIds(failedMatchIds)
                .message("수집 완료: 신규 경기 " + savedMatchCount + "개, 참가자 " + savedParticipantCount + "명 저장")
                .build();
    }

    public CollectionResultResponse collectMatches(String gameName, String tagLine, int count) {
        return collectRecentMatchesByRiotId(gameName, tagLine, count);
    }

    @Transactional
    public boolean saveMatchDetailIfAbsent(String matchId, JsonNode root) {
        if (matchId == null || matchId.isBlank()) {
            return false;
        }

        if (root == null || root.isMissingNode()) {
            return false;
        }

        if (matchRepository.existsByMatchId(matchId)) {
            return false;
        }

        try {
            saveMatchDetail(matchId, root);
            return true;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }

    private List<String> fetchMatchIds(String puuid, int count, int queueId) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(matchBaseUrl)
                .pathSegment(
                        "lol",
                        "match",
                        "v5",
                        "matches",
                        "by-puuid",
                        puuid,
                        "ids"
                )
                .queryParam("queue", queueId)
                .queryParam("start", 0)
                .queryParam("count", count)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String[]> response = exchangeWithRiot(uri.toString(), String[].class);

        String[] body = response.getBody();
        if (body == null) {
            return List.of();
        }

        return List.of(body);
    }

    private JsonNode fetchMatchDetail(String matchId) {
        String url = matchBaseUrl + "/lol/match/v5/matches/" + matchId;
        ResponseEntity<JsonNode> response = exchangeWithRiot(url, JsonNode.class);
        JsonNode body = response.getBody();
        if (body == null || body.isMissingNode()) {
            throw new IllegalStateException("매치 상세 응답이 비어 있습니다.");
        }
        return body;
    }

    private <T> ResponseEntity<T> exchangeWithRiot(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            sleepForRateLimit();
            return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        } catch (HttpClientErrorException.TooManyRequests e) {
            long retryAfterSeconds = parseRetryAfterSeconds(e.getResponseHeaders());
            sleep(retryAfterSeconds * 1000L + 500L);
            return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        }
    }

    private long parseRetryAfterSeconds(HttpHeaders headers) {
        if (headers == null) return 3L;
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter == null) return 3L;
        try {
            return Long.parseLong(retryAfter);
        } catch (NumberFormatException e) {
            return 3L;
        }
    }

    private void sleepForRateLimit() {
        sleep(requestDelayMs);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Riot API 호출 대기 중 인터럽트가 발생했습니다.", e);
        }
    }

    private int saveMatchDetail(String matchId, JsonNode root) {
        JsonNode info = root.path("info");
        JsonNode metadata = root.path("metadata");

        MatchEntity match = MatchEntity.builder()
                .matchId(matchId)
                .gameVersion(textOrNull(info, "gameVersion"))
                .queueId(intOrNull(info, "queueId"))
                .gameCreation(longOrNull(info, "gameCreation"))
                .gameDuration(intOrNull(info, "gameDuration"))
                .platformId(textOrNull(info, "platformId"))
                .winningTeamId(findWinningTeamId(info.path("teams")))
                .build();

        matchRepository.save(match);

        List<MatchParticipant> participants = new ArrayList<>();
        for (JsonNode participant : info.path("participants")) {
            String puuid = textOrNull(participant, "puuid");
            if (puuid == null || puuid.isBlank()) {
                continue;
            }

            JsonNode styles = participant.path("perks").path("styles");
            Integer primaryStyleId = styles.size() > 0 ? intOrNull(styles.get(0), "style") : null;
            Integer subStyleId = styles.size() > 1 ? intOrNull(styles.get(1), "style") : null;
            Integer mainRuneId = null;
            if (styles.size() > 0 && styles.get(0).path("selections").size() > 0) {
                mainRuneId = intOrNull(styles.get(0).path("selections").get(0), "perk");
            }

            MatchParticipant row = MatchParticipant.builder()
                    .matchId(matchId)
                    .puuid(puuid)
                    .riotGameName(textOrNull(participant, "riotIdGameName"))
                    .riotTagLine(textOrNull(participant, "riotIdTagline"))
                    .summonerName(textOrNull(participant, "summonerName"))
                    .championId(intOrNull(participant, "championId"))
                    .championName(textOrNull(participant, "championName"))
                    .teamId(intOrNull(participant, "teamId"))
                    .teamPosition(textOrNull(participant, "teamPosition"))
                    .individualPosition(textOrNull(participant, "individualPosition"))
                    .win(boolOrNull(participant, "win"))
                    .kills(intOrNull(participant, "kills"))
                    .deaths(intOrNull(participant, "deaths"))
                    .assists(intOrNull(participant, "assists"))
                    .totalDamageDealtToChampions(intOrNull(participant, "totalDamageDealtToChampions"))
                    .totalDamageTaken(intOrNull(participant, "totalDamageTaken"))
                    .goldEarned(intOrNull(participant, "goldEarned"))
                    .totalMinionsKilled(intOrNull(participant, "totalMinionsKilled"))
                    .neutralMinionsKilled(intOrNull(participant, "neutralMinionsKilled"))
                    .visionScore(intOrNull(participant, "visionScore"))
                    .wardsPlaced(intOrNull(participant, "wardsPlaced"))
                    .wardsKilled(intOrNull(participant, "wardsKilled"))
                    .summoner1Id(intOrNull(participant, "summoner1Id"))
                    .summoner2Id(intOrNull(participant, "summoner2Id"))
                    .item0(intOrNull(participant, "item0"))
                    .item1(intOrNull(participant, "item1"))
                    .item2(intOrNull(participant, "item2"))
                    .item3(intOrNull(participant, "item3"))
                    .item4(intOrNull(participant, "item4"))
                    .item5(intOrNull(participant, "item5"))
                    .item6(intOrNull(participant, "item6"))
                    .primaryStyleId(primaryStyleId)
                    .subStyleId(subStyleId)
                    .mainRuneId(mainRuneId)
                    .build();

            participants.add(row);
        }

        matchParticipantRepository.saveAll(participants);
        return participants.size();
    }

    private Integer findWinningTeamId(JsonNode teams) {
        for (JsonNode team : teams) {
            if (team.path("win").asBoolean(false)) {
                return intOrNull(team, "teamId");
            }
        }
        return null;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) return null;
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private Integer intOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) return null;
        return value.asInt();
    }

    private Long longOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) return null;
        return value.asLong();
    }

    private Boolean boolOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) return null;
        return value.asBoolean();
    }
}
