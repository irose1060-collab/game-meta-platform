package com.game.backend.service;

import com.game.backend.dto.HomeMetaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeMetaService {

    private final RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    public HomeMetaResponse getHomeMeta() {
        try {
            String versionUrl = "https://ddragon.leagueoflegends.com/api/versions.json";
            List<String> versions = restTemplate.getForObject(versionUrl, List.class);

            String latestVersion = versions != null && !versions.isEmpty()
                    ? versions.get(0)
                    : "14.21.1";

            String championUrl =
                    "https://ddragon.leagueoflegends.com/cdn/"
                            + latestVersion
                            + "/data/ko_KR/champion.json";

            Map<String, Object> championResponse =
                    restTemplate.getForObject(championUrl, Map.class);

            String name = "Aatrox";
            String nameKr = "아트록스";
            String championKey = "266";
            String imageUrl =
                    "https://ddragon.leagueoflegends.com/cdn/"
                            + latestVersion
                            + "/img/champion/Aatrox.png";

            if (championResponse != null && championResponse.get("data") instanceof Map<?, ?> dataMap) {
                Object aatroxObject = dataMap.get("Aatrox");

                if (aatroxObject instanceof LinkedHashMap<?, ?> aatrox) {
                    Object idValue = aatrox.get("id");
                    Object nameValue = aatrox.get("name");
                    Object keyValue = aatrox.get("key");

                    name = idValue != null ? String.valueOf(idValue) : "Aatrox";
                    nameKr = nameValue != null ? String.valueOf(nameValue) : "아트록스";
                    championKey = keyValue != null ? String.valueOf(keyValue) : "266";

                    imageUrl =
                            "https://ddragon.leagueoflegends.com/cdn/"
                                    + latestVersion
                                    + "/img/champion/"
                                    + name
                                    + ".png";
                }
            }

            return buildResponse(
                    latestVersion,
                    name,
                    nameKr,
                    championKey,
                    imageUrl,
                    "Riot Data Dragon"
            );
        } catch (Exception e) {
            return buildResponse(
                    "14.21.1",
                    "Aatrox",
                    "아트록스",
                    "266",
                    "https://ddragon.leagueoflegends.com/cdn/14.21.1/img/champion/Aatrox.png",
                    "Fallback Data"
            );
        }
    }

    private HomeMetaResponse buildResponse(
            String version,
            String name,
            String nameKr,
            String championKey,
            String imageUrl,
            String source
    ) {
        return HomeMetaResponse.builder()
                .hotChampion(
                        HomeMetaResponse.HotChampion.builder()
                                .name(name)
                                .nameKr(nameKr)
                                .championKey(championKey)
                                .position("TOP")
                                .imageUrl(imageUrl)
                                .winRate(53.4)
                                .pickRate(14.2)
                                .banRate(8.7)
                                .source(source)
                                .build()
                )
                .patchSummary(
                        HomeMetaResponse.PatchSummary.builder()
                                .version(version)
                                .summary("Riot Data Dragon 최신 버전 기반 챔피언 데이터를 불러왔습니다. 통계값은 추후 match 데이터 수집 후 직접 계산 구조로 확장됩니다.")
                                .detail1("최신 버전: " + version)
                                .detail2("챔피언 정적 데이터 출처: Riot Data Dragon")
                                .detail3("승률·픽률·밴률은 DB 통계 테이블과 연동 예정")
                                .source(source)
                                .build()
                )
                .teamCompSummary(
                        HomeMetaResponse.TeamCompSummary.builder()
                                .apStatus("부족")
                                .apRatio(35)
                                .ccStatus("보통")
                                .ccScore(60)
                                .expectedWinRate(48)
                                .source("Backend Analysis API")
                                .build()
                )
                .aiFeedbackSummary(
                        HomeMetaResponse.AiFeedbackSummary.builder()
                                .feedback1("최근 경기 기반 플레이 스타일 분석")
                                .feedback2("패배 원인 자동 요약 제공")
                                .feedback3("챔피언별 맞춤 개선 포인트")
                                .source("AI Feedback API")
                                .build()
                )
                .build();
    }
}