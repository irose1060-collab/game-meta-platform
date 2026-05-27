package com.game.backend.service;

import com.game.backend.dto.AssetDto;
import com.game.backend.dto.ChampionDetailResponse;
import com.game.backend.dto.ChampionListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChampionService {

    private final RestTemplate restTemplate;
    private static final String DDRAGON_VERSION_URL = "https://ddragon.leagueoflegends.com/api/versions.json";

    @SuppressWarnings("unchecked")
    public List<ChampionListResponse> getChampionList() {
        String version = getLatestVersion();
        Map<String, Object> championResponse = restTemplate.getForObject(championListUrl(version), Map.class);

        if (championResponse == null || !(championResponse.get("data") instanceof Map<?, ?> dataMap)) {
            return List.of();
        }

        return dataMap.values()
                .stream()
                .filter(value -> value instanceof Map)
                .map(value -> toChampionListResponse(version, (Map<String, Object>) value))
                .sorted(Comparator.comparing(ChampionListResponse::getNameKr))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public ChampionDetailResponse getChampionDetail(String championId) {
        String version = getLatestVersion();
        Map<String, Object> detailResponse = restTemplate.getForObject(championDetailUrl(version, championId), Map.class);

        if (detailResponse == null || !(detailResponse.get("data") instanceof Map<?, ?> dataMap)) {
            throw new RuntimeException("챔피언 정보를 찾을 수 없습니다.");
        }

        Object championObject = dataMap.get(championId);
        if (!(championObject instanceof Map<?, ?> champion)) {
            throw new RuntimeException("챔피언 정보를 찾을 수 없습니다.");
        }

        return toChampionDetailResponse(version, (Map<String, Object>) champion);
    }

    @SuppressWarnings("unchecked")
    private ChampionListResponse toChampionListResponse(String version, Map<String, Object> champion) {
        String id = stringValue(champion.get("id"), "Unknown");
        String key = stringValue(champion.get("key"), "0");
        String nameKr = stringValue(champion.get("name"), id);
        String title = stringValue(champion.get("title"), "");
        String blurb = stringValue(champion.get("blurb"), "");
        String partype = stringValue(champion.get("partype"), "");
        List<String> tags = listValue(champion.get("tags"));

        String imageFull = imageFull(champion, id + ".png");
        String imageUrl = championImageUrl(version, imageFull);

        RateSet rates = makeRates(key);

        // 💡 여기를 수정: 태그뿐만 아니라 챔피언 id(영문이름)도 함께 넘겨줌
        String position = inferPosition(id, tags);

        String tier = makeTier(rates.winRate, rates.pickRate);
        int difficulty = 0;
        if (champion.get("info") instanceof Map<?, ?> infoMap) {
            difficulty = intValue(infoMap.get("difficulty"), 0);
        }

        return ChampionListResponse.builder()
                .id(id)
                .key(key)
                .nameKr(nameKr)
                .title(title)
                .blurb(blurb)
                .imageUrl(imageUrl)
                .tags(tags)
                .partype(partype)
                .position(position)
                .tier(tier)
                .winRate(rates.winRate)
                .pickRate(rates.pickRate)
                .banRate(rates.banRate)
                .difficulty(difficulty)
                .build();
    }

    @SuppressWarnings("unchecked")
    private ChampionDetailResponse toChampionDetailResponse(String version, Map<String, Object> champion) {
        ChampionListResponse base = toChampionListResponse(version, champion);
        String lore = stringValue(champion.get("lore"), base.getBlurb());

        ChampionDetailResponse.Info info = ChampionDetailResponse.Info.builder()
                .attack(0).defense(0).magic(0).difficulty(base.getDifficulty()).build();

        if (champion.get("info") instanceof Map<?, ?> infoMap) {
            info = ChampionDetailResponse.Info.builder()
                    .attack(intValue(infoMap.get("attack"), 0))
                    .defense(intValue(infoMap.get("defense"), 0))
                    .magic(intValue(infoMap.get("magic"), 0))
                    .difficulty(intValue(infoMap.get("difficulty"), base.getDifficulty()))
                    .build();
        }

        ChampionDetailResponse.Stats stats = ChampionDetailResponse.Stats.builder().build();
        if (champion.get("stats") instanceof Map<?, ?> statsMap) {
            stats = ChampionDetailResponse.Stats.builder()
                    .hp(doubleValue(statsMap.get("hp"), 0))
                    .hpPerLevel(doubleValue(statsMap.get("hpperlevel"), 0))
                    .mp(doubleValue(statsMap.get("mp"), 0))
                    .mpPerLevel(doubleValue(statsMap.get("mpperlevel"), 0))
                    .moveSpeed(doubleValue(statsMap.get("movespeed"), 0))
                    .armor(doubleValue(statsMap.get("armor"), 0))
                    .armorPerLevel(doubleValue(statsMap.get("armorperlevel"), 0))
                    .spellBlock(doubleValue(statsMap.get("spellblock"), 0))
                    .attackRange(doubleValue(statsMap.get("attackrange"), 0))
                    .attackDamage(doubleValue(statsMap.get("attackdamage"), 0))
                    .attackSpeed(doubleValue(statsMap.get("attackspeed"), 0))
                    .build();
        }

        ChampionDetailResponse.Passive passive = null;
        if (champion.get("passive") instanceof Map<?, ?> passiveMap) {
            String passiveImage = "";
            Object passiveImageObj = passiveMap.get("image");
            if (passiveImageObj instanceof Map<?, ?> imageMap) {
                passiveImage = stringValue(imageMap.get("full"), "");
            }
            passive = ChampionDetailResponse.Passive.builder()
                    .name(stringValue(passiveMap.get("name"), "패시브"))
                    .description(cleanDescription(stringValue(passiveMap.get("description"), "")))
                    .imageUrl(passiveImage.isBlank() ? "" : passiveImageUrl(version, passiveImage))
                    .build();
        }

        List<ChampionDetailResponse.Skill> spells = new ArrayList<>();
        if (champion.get("spells") instanceof List<?> spellList) {
            for (Object spellObj : spellList) {
                if (spellObj instanceof Map<?, ?> spellMap) {
                    String skillImage = "";
                    Object skillImageObj = spellMap.get("image");
                    if (skillImageObj instanceof Map<?, ?> imageMap) {
                        skillImage = stringValue(imageMap.get("full"), "");
                    }
                    spells.add(ChampionDetailResponse.Skill.builder()
                            .id(stringValue(spellMap.get("id"), ""))
                            .name(stringValue(spellMap.get("name"), ""))
                            .description(cleanDescription(stringValue(spellMap.get("description"), "")))
                            .cooldown(stringValue(spellMap.get("cooldownBurn"), ""))
                            .cost(stringValue(spellMap.get("costBurn"), ""))
                            .imageUrl(skillImage.isBlank() ? "" : spellImageUrl(version, skillImage))
                            .build());
                }
            }
        }

        List<String> allytips = listValue(champion.get("allytips"));
        List<String> enemytips = listValue(champion.get("enemytips"));

        return ChampionDetailResponse.builder()
                .id(base.getId())
                .key(base.getKey())
                .nameKr(base.getNameKr())
                .title(base.getTitle())
                .lore(lore)
                .blurb(base.getBlurb())
                .imageUrl(base.getImageUrl())
                .tags(base.getTags())
                .partype(base.getPartype())
                .position(base.getPosition())
                .tier(base.getTier())
                .winRate(base.getWinRate())
                .pickRate(base.getPickRate())
                .banRate(base.getBanRate())
                .info(info)
                .stats(stats)
                .passive(passive)
                .spells(spells)
                .allytips(allytips)
                .enemytips(enemytips)
                .recommendation(makeRecommendation(version, base))
                .build();
    }

    private ChampionDetailResponse.Recommendation makeRecommendation(String version, ChampionListResponse champion) {
        Set<String> tags = new HashSet<>(champion.getTags());

        if (tags.contains("Marksman")) {
            return ChampionDetailResponse.Recommendation.builder()
                    .summonerSpells(List.of(spell(version, "4", "점멸", "SummonerFlash.png"), spell(version, "7", "회복", "SummonerHeal.png")))
                    .runes(List.of(rune("정밀", "집중 공격", "perk-images/Styles/Precision/PressTheAttack/PressTheAttack.png"), rune("정밀", "전설: 민첩함", "perk-images/Styles/Precision/LegendAlacrity/LegendAlacrity.png"), rune("마법", "폭풍의 결집", "perk-images/Styles/Sorcery/GatheringStorm/GatheringStorm.png")))
                    .items(List.of(item(version, "6672", "크라켄 학살자"), item(version, "3031", "무한의 대검"), item(version, "3006", "광전사의 군화"), item(version, "3036", "도미닉 경의 인사")))
                    .counters(genericCounters("Malphite", "말파이트", "Leona", "레오나", "Rammus", "람머스", version))
                    .note("원거리 딜러형 챔피언 기준 임시 추천입니다. 실제 추천은 추후 match 데이터 기반으로 계산됩니다.")
                    .build();
        }

        if (tags.contains("Mage")) {
            return ChampionDetailResponse.Recommendation.builder()
                    .summonerSpells(List.of(spell(version, "4", "점멸", "SummonerFlash.png"), spell(version, "14", "점화", "SummonerDot.png")))
                    .runes(List.of(rune("마법", "신비로운 유성", "perk-images/Styles/Sorcery/ArcaneComet/ArcaneComet.png"), rune("마법", "마나순환 팔찌", "perk-images/Styles/Sorcery/ManaflowBand/ManaflowBand.png"), rune("지배", "피의 맛", "perk-images/Styles/Domination/TasteOfBlood/GreenTerror_TasteOfBlood.png")))
                    .items(List.of(item(version, "6655", "루덴의 동반자"), item(version, "3020", "마법사의 신발"), item(version, "3089", "라바돈의 죽음모자"), item(version, "3157", "존야의 모래시계")))
                    .counters(genericCounters("Zed", "제드", "Fizz", "피즈", "Kassadin", "카사딘", version))
                    .note("마법사형 챔피언 기준 임시 추천입니다. 실제 추천은 추후 match 데이터 기반으로 계산됩니다.")
                    .build();
        }

        if (tags.contains("Assassin")) {
            return ChampionDetailResponse.Recommendation.builder()
                    .summonerSpells(List.of(spell(version, "4", "점멸", "SummonerFlash.png"), spell(version, "14", "점화", "SummonerDot.png")))
                    .runes(List.of(rune("지배", "감전", "perk-images/Styles/Domination/Electrocute/Electrocute.png"), rune("지배", "돌발 일격", "perk-images/Styles/Domination/SuddenImpact/SuddenImpact.png"), rune("정밀", "최후의 일격", "perk-images/Styles/Precision/CoupDeGrace/CoupDeGrace.png")))
                    .items(List.of(item(version, "3142", "요우무의 유령검"), item(version, "6692", "월식"), item(version, "3814", "밤의 끝자락"), item(version, "3158", "명석함의 아이오니아 장화")))
                    .counters(genericCounters("Malphite", "말파이트", "Lissandra", "리산드라", "Leona", "레오나", version))
                    .note("암살자형 챔피언 기준 임시 추천입니다. 실제 추천은 추후 match 데이터 기반으로 계산됩니다.")
                    .build();
        }

        if (tags.contains("Tank")) {
            return ChampionDetailResponse.Recommendation.builder()
                    .summonerSpells(List.of(spell(version, "4", "점멸", "SummonerFlash.png"), spell(version, "12", "순간이동", "SummonerTeleport.png")))
                    .runes(List.of(rune("결의", "착취의 손아귀", "perk-images/Styles/Resolve/GraspOfTheUndying/GraspOfTheUndying.png"), rune("결의", "철거", "perk-images/Styles/Resolve/Demolish/Demolish.png"), rune("결의", "소생", "perk-images/Styles/Resolve/Revitalize/Revitalize.png")))
                    .items(List.of(item(version, "3068", "태양불꽃 방패"), item(version, "3075", "가시 갑옷"), item(version, "3047", "판금 장화"), item(version, "6665", "해신 작쇼")))
                    .counters(genericCounters("Vayne", "베인", "Fiora", "피오라", "Gwen", "그웬", version))
                    .note("탱커형 챔피언 기준 임시 추천입니다. 실제 추천은 추후 match 데이터 기반으로 계산됩니다.")
                    .build();
        }

        if (tags.contains("Support")) {
            return ChampionDetailResponse.Recommendation.builder()
                    .summonerSpells(List.of(spell(version, "4", "점멸", "SummonerFlash.png"), spell(version, "3", "탈진", "SummonerExhaust.png")))
                    .runes(List.of(rune("결의", "수호자", "perk-images/Styles/Resolve/Guardian/Guardian.png"), rune("영감", "비스킷 배달", "perk-images/Styles/Inspiration/BiscuitDelivery/BiscuitDelivery.png"), rune("결의", "소생", "perk-images/Styles/Resolve/Revitalize/Revitalize.png")))
                    .items(List.of(item(version, "3190", "강철의 솔라리 펜던트"), item(version, "3107", "구원"), item(version, "3222", "미카엘의 축복"), item(version, "3117", "기동력의 장화")))
                    .counters(genericCounters("Morgana", "모르가나", "Blitzcrank", "블리츠크랭크", "Pyke", "파이크", version))
                    .note("서포터형 챔피언 기준 임시 추천입니다. 실제 추천은 추후 match 데이터 기반으로 계산됩니다.")
                    .build();
        }

        return ChampionDetailResponse.Recommendation.builder()
                .summonerSpells(List.of(spell(version, "4", "점멸", "SummonerFlash.png"), spell(version, "12", "순간이동", "SummonerTeleport.png")))
                .runes(List.of(rune("정밀", "정복자", "perk-images/Styles/Precision/Conqueror/Conqueror.png"), rune("결의", "뼈 방패", "perk-images/Styles/Resolve/BonePlating/BonePlating.png")))
                .items(List.of(item(version, "3078", "삼위일체"), item(version, "3071", "칠흑의 양날 도끼"), item(version, "6333", "죽음의 무도")))
                .counters(genericCounters("Malphite", "말파이트", "Jax", "잭스", "Fiora", "피오라", version))
                .note("기본 전사형 챔피언 기준 임시 추천입니다. 실제 추천은 추후 match 데이터 기반으로 계산됩니다.")
                .build();
    }

    private List<AssetDto> genericCounters(String id1, String name1, String id2, String name2, String id3, String name3, String version) {
        return List.of(
                AssetDto.builder().id(id1).name(name1).description("상성 주의 챔피언").imageUrl(championImageUrl(version, id1 + ".png")).build(),
                AssetDto.builder().id(id2).name(name2).description("라인전/교전 주의").imageUrl(championImageUrl(version, id2 + ".png")).build(),
                AssetDto.builder().id(id3).name(name3).description("픽 단계에서 고려").imageUrl(championImageUrl(version, id3 + ".png")).build()
        );
    }

    private AssetDto spell(String version, String id, String name, String imageFull) {
        return AssetDto.builder().id(id).name(name).description("추천 소환사 주문").imageUrl("https://ddragon.leagueoflegends.com/cdn/" + version + "/img/spell/" + imageFull).build();
    }

    private AssetDto item(String version, String id, String name) {
        return AssetDto.builder().id(id).name(name).description("추천 아이템").imageUrl("https://ddragon.leagueoflegends.com/cdn/" + version + "/img/item/" + id + ".png").build();
    }

    private AssetDto rune(String style, String name, String iconPath) {
        return AssetDto.builder().id(name).name(name).description(style).imageUrl("https://ddragon.leagueoflegends.com/cdn/img/" + iconPath).build();
    }

    private String getLatestVersion() {
        List<String> versions = restTemplate.getForObject(DDRAGON_VERSION_URL, List.class);
        return versions != null && !versions.isEmpty() ? versions.get(0) : "14.21.1";
    }

    private String championListUrl(String version) {
        return "https://ddragon.leagueoflegends.com/cdn/" + version + "/data/ko_KR/champion.json";
    }

    private String championDetailUrl(String version, String championId) {
        return "https://ddragon.leagueoflegends.com/cdn/" + version + "/data/ko_KR/champion/" + championId + ".json";
    }

    private String championImageUrl(String version, String imageFull) {
        return "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/champion/" + imageFull;
    }

    private String passiveImageUrl(String version, String imageFull) {
        return "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/passive/" + imageFull;
    }

    private String spellImageUrl(String version, String imageFull) {
        return "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/spell/" + imageFull;
    }

    private String imageFull(Map<String, Object> champion, String fallback) {
        if (champion.get("image") instanceof Map<?, ?> imageMap) {
            Object full = imageMap.get("full");
            return full != null ? String.valueOf(full) : fallback;
        }
        return fallback;
    }

    private String inferPosition(String id, List<String> tags) {
        // 💡 대표적인 정글 챔피언 리스트 선언 (Riot Data Dragon ID 기준)
        Set<String> junglers = Set.of(
                "Amumu", "Belveth", "Briar", "Elise", "Evelynn", "Fiddlesticks", "Graves",
                "Hecarim", "Ivern", "JarvanIV", "Karthus", "Khazix", "Kindred", "LeeSin",
                "Lillia", "MasterYi", "Nidalee", "Nocturne", "Nunu", "Olaf", "Rammus",
                "RekSai", "Rengar", "Sejuani", "Shaco", "Skarner", "Taliyah", "Udyr",
                "Vi", "Viego", "Volibear", "Warwick", "XinZhao", "Zac"
        );

        // 정글러 리스트에 포함되어 있다면 JUNGLE 반환
        if (junglers.contains(id)) return "JUNGLE";

        if (tags.contains("Support")) return "SUPPORT";
        if (tags.contains("Marksman")) return "ADC";
        if (tags.contains("Assassin")) return "MID";
        if (tags.contains("Mage")) return "MID";
        if (tags.contains("Tank")) return "TOP";
        if (tags.contains("Fighter")) return "TOP";
        return "FLEX";
    }

    private String makeTier(double winRate, double pickRate) {
        double score = winRate + pickRate * 0.35;
        if (score >= 58) return "S";
        if (score >= 55) return "A";
        if (score >= 52) return "B";
        if (score >= 49) return "C";
        return "D";
    }

    private RateSet makeRates(String key) {
        int seed = Math.abs(key.hashCode());
        double winRate = round1(47.0 + (seed % 120) / 10.0);
        double pickRate = round1(2.0 + ((seed / 7) % 180) / 10.0);
        double banRate = round1(0.5 + ((seed / 13) % 160) / 10.0);
        return new RateSet(winRate, pickRate, banRate);
    }

    private String cleanDescription(String value) {
        if (value == null) return "";
        return value.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
    }

    private String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    private int intValue(Object value, int fallback) {
        try { return value != null ? Integer.parseInt(String.valueOf(value)) : fallback; }
        catch (Exception e) { return fallback; }
    }

    private double doubleValue(Object value, double fallback) {
        try { return value != null ? Double.parseDouble(String.valueOf(value)) : fallback; }
        catch (Exception e) { return fallback; }
    }

    private List<String> listValue(Object value) {
        if (value instanceof List<?> rawList) {
            return rawList.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record RateSet(double winRate, double pickRate, double banRate) {}
}
