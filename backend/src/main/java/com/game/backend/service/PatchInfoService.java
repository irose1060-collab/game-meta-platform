package com.game.backend.service;

import com.game.backend.dto.PatchInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PatchInfoService {

    private final RestTemplate restTemplate;
    private static final String VERSION_URL = "https://ddragon.leagueoflegends.com/api/versions.json";

    @SuppressWarnings("unchecked")
    public PatchInfoResponse getPatchInfo() {
        try {
            List<String> versions = restTemplate.getForObject(VERSION_URL, List.class);
            if (versions == null || versions.isEmpty()) {
                return fallback();
            }

            String latest = versions.get(0);
            String previous = versions.size() > 1 ? versions.get(1) : latest;

            Map<String, Object> latestData = fetchChampionData(latest);
            Map<String, Object> previousData = fetchChampionData(previous);

            List<PatchInfoResponse.PatchChampionInfo> newChampions = findNewChampions(latest, latestData, previousData);
            List<PatchInfoResponse.PatchChampionInfo> removedChampions = findRemovedChampions(previous, latestData, previousData);
            List<PatchInfoResponse.PatchChampionInfo> updatedChampions = findUpdatedChampions(latest, latestData, previousData);

            List<String> summaryLines = new ArrayList<>();
            summaryLines.add("최신 Data Dragon 버전: " + latest);
            summaryLines.add("비교 기준 이전 버전: " + previous);
            summaryLines.add("최신 버전 챔피언 수: " + latestData.size());
            summaryLines.add("신규 챔피언 수: " + newChampions.size());
            summaryLines.add("변경 감지 챔피언 수: " + updatedChampions.size());
            if (newChampions.isEmpty() && removedChampions.isEmpty() && updatedChampions.isEmpty()) {
                summaryLines.add("champion.json 기준 큰 변경 사항은 감지되지 않았습니다.");
            }

            return PatchInfoResponse.builder()
                    .latestVersion(latest)
                    .previousVersion(previous)
                    .source("Riot Data Dragon")
                    .championCount(latestData.size())
                    .previousChampionCount(previousData.size())
                    .recentVersions(versions.stream().limit(8).toList())
                    .newChampions(newChampions)
                    .removedChampions(removedChampions)
                    .updatedChampions(updatedChampions)
                    .summaryLines(summaryLines)
                    .build();
        } catch (Exception e) {
            return fallback();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchChampionData(String version) {
        String url = "https://ddragon.leagueoflegends.com/cdn/" + version + "/data/ko_KR/champion.json";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            if (entry.getKey() != null && entry.getValue() instanceof Map<?, ?>) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private List<PatchInfoResponse.PatchChampionInfo> findNewChampions(
            String version,
            Map<String, Object> latest,
            Map<String, Object> previous
    ) {
        List<PatchInfoResponse.PatchChampionInfo> result = new ArrayList<>();
        for (String id : latest.keySet()) {
            if (!previous.containsKey(id)) {
                result.add(toChampionInfo(version, id, latest.get(id), "신규", "이전 버전에 없던 챔피언입니다."));
            }
        }
        return result;
    }

    private List<PatchInfoResponse.PatchChampionInfo> findRemovedChampions(
            String version,
            Map<String, Object> latest,
            Map<String, Object> previous
    ) {
        List<PatchInfoResponse.PatchChampionInfo> result = new ArrayList<>();
        for (String id : previous.keySet()) {
            if (!latest.containsKey(id)) {
                result.add(toChampionInfo(version, id, previous.get(id), "삭제", "최신 버전에서 제거된 챔피언 데이터입니다."));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<PatchInfoResponse.PatchChampionInfo> findUpdatedChampions(
            String version,
            Map<String, Object> latest,
            Map<String, Object> previous
    ) {
        List<PatchInfoResponse.PatchChampionInfo> result = new ArrayList<>();
        for (String id : latest.keySet()) {
            if (!previous.containsKey(id)) continue;

            Object latestObject = latest.get(id);
            Object previousObject = previous.get(id);

            if (!(latestObject instanceof Map<?, ?> latestChampion)
                    || !(previousObject instanceof Map<?, ?> previousChampion)) {
                continue;
            }

            String latestTitle = value(latestChampion.get("title"));
            String previousTitle = value(previousChampion.get("title"));
            String latestBlurb = value(latestChampion.get("blurb"));
            String previousBlurb = value(previousChampion.get("blurb"));

            if (!Objects.equals(latestTitle, previousTitle) || !Objects.equals(latestBlurb, previousBlurb)) {
                result.add(toChampionInfo(version, id, latestObject, "정보 변경", "챔피언 설명 또는 칭호 변경이 감지되었습니다."));
            }

            if (result.size() >= 12) break;
        }
        return result;
    }

    private PatchInfoResponse.PatchChampionInfo toChampionInfo(
            String version,
            String fallbackId,
            Object championObject,
            String changeType,
            String description
    ) {
        if (!(championObject instanceof Map<?, ?> champion)) {
            return PatchInfoResponse.PatchChampionInfo.builder()
                    .id(fallbackId)
                    .key("")
                    .nameKr(fallbackId)
                    .title("")
                    .imageUrl("")
                    .changeType(changeType)
                    .description(description)
                    .build();
        }

        String id = valueOr(champion.get("id"), fallbackId);
        String key = valueOr(champion.get("key"), "");
        String nameKr = valueOr(champion.get("name"), id);
        String title = valueOr(champion.get("title"), "");
        String imageFull = id + ".png";

        if (champion.get("image") instanceof Map<?, ?> imageMap && imageMap.get("full") != null) {
            imageFull = String.valueOf(imageMap.get("full"));
        }

        String imageUrl = "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/champion/" + imageFull;

        return PatchInfoResponse.PatchChampionInfo.builder()
                .id(id)
                .key(key)
                .nameKr(nameKr)
                .title(title)
                .imageUrl(imageUrl)
                .changeType(changeType)
                .description(description)
                .build();
    }

    private PatchInfoResponse fallback() {
        return PatchInfoResponse.builder()
                .latestVersion("unknown")
                .previousVersion("unknown")
                .source("Fallback")
                .championCount(0)
                .previousChampionCount(0)
                .recentVersions(List.of())
                .newChampions(List.of())
                .removedChampions(List.of())
                .updatedChampions(List.of())
                .summaryLines(List.of("패치 정보를 불러오지 못했습니다.", "네트워크 또는 Data Dragon 응답을 확인해야 합니다."))
                .build();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String valueOr(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
