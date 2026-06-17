package com.game.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.backend.dto.AiWinAnalysisResponse;
import com.game.backend.dto.GeminiWinAnalysisRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiWinAnalysisService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String geminiModel;

    public AiWinAnalysisResponse analyze(GeminiWinAnalysisRequest request) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "GEMINI_API_KEY 환경변수가 설정되지 않았습니다."
            );
        }

        List<GeminiWinAnalysisRequest.MatchItem> matches =
                request.matches() == null ? List.of() : request.matches();

        if (matches.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "분석할 최근 경기 데이터가 없습니다."
            );
        }

        LocalStats stats = calculateLocalStats(request, matches);

        GeminiTextResult geminiTextResult;

        try {
            geminiTextResult = callGemini(request, matches, stats);
        } catch (Exception e) {
            geminiTextResult = buildFallbackTextResult(stats);
        }

        return AiWinAnalysisResponse.builder()
                .gameName(nullToDefault(request.gameName(), "Unknown"))
                .tagLine(nullToDefault(request.tagLine(), "KR1"))
                .totalMatches(stats.totalMatches())
                .wins(stats.wins())
                .losses(stats.losses())
                .winRate(stats.winRate())
                .averageKda(stats.averageKda())
                .averageKills(stats.averageKills())
                .averageDeaths(stats.averageDeaths())
                .averageAssists(stats.averageAssists())
                .averageDamage(stats.averageDamage())
                .averageCs(stats.averageCs())
                .averageGold(stats.averageGold())
                .summary(geminiTextResult.summary())
                .strengths(geminiTextResult.strengths())
                .weaknesses(geminiTextResult.weaknesses())
                .recommendations(geminiTextResult.recommendations())
                .build();
        }

    private GeminiTextResult callGemini(
            GeminiWinAnalysisRequest request,
            List<GeminiWinAnalysisRequest.MatchItem> matches,
            LocalStats stats
    ) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel
                + ":generateContent";

        String prompt = buildPrompt(request, matches, stats);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 2048,
                        "responseMimeType", "application/json"
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Gemini API 응답이 비정상입니다.");
        }

        String text = extractGeminiText(response.getBody());
        return parseGeminiJson(text);
    }

    private String buildPrompt(
            GeminiWinAnalysisRequest request,
            List<GeminiWinAnalysisRequest.MatchItem> matches,
            LocalStats stats
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                너는 리그 오브 레전드 전적 분석 전문가다.
                아래 최근 경기 데이터를 보고 플레이어의 승률 개선 리포트를 한국어로 작성해라.

                반드시 완성된 JSON 객체 하나만 반환해라.
                마크다운, 코드블록, 추가 설명, 줄임표를 절대 쓰지 마라.
                문장은 너무 길게 쓰지 말고 각 항목은 80자 이내로 작성해라.

                {
                  "summary": "전체 요약 2~3문장",
                  "strengths": ["강점 1", "강점 2", "강점 3"],
                  "weaknesses": ["약점 1", "약점 2", "약점 3"],
                  "recommendations": ["개선 제안 1", "개선 제안 2", "개선 제안 3"]
                }

                분석 기준:
                - 단순히 KDA만 보지 말고 승패, 데스, CS, 딜량, 챔피언, 포지션을 종합해라.
                - 너무 일반적인 말보다 실제 플레이 개선 조언처럼 작성해라.
                - 말투는 게임 분석 사이트에 어울리게 단정하고 직관적으로 작성해라.

                """);

        sb.append("플레이어: ")
                .append(nullToDefault(request.gameName(), "Unknown"))
                .append("#")
                .append(nullToDefault(request.tagLine(), "KR1"))
                .append("\n");

        sb.append("최근 경기 수: ").append(stats.totalMatches()).append("\n");
        sb.append("승패: ").append(stats.wins()).append("승 ").append(stats.losses()).append("패\n");
        sb.append("승률: ").append(stats.winRate()).append("%\n");
        sb.append("평균 KDA: ").append(stats.averageKda()).append("\n");
        sb.append("평균 킬/데스/어시: ")
                .append(stats.averageKills()).append("/")
                .append(stats.averageDeaths()).append("/")
                .append(stats.averageAssists()).append("\n");
        sb.append("평균 딜량: ").append(stats.averageDamage()).append("\n");
        sb.append("평균 CS: ").append(stats.averageCs()).append("\n");
        sb.append("평균 골드: ").append(stats.averageGold()).append("\n\n");

        sb.append("경기 목록:\n");

        for (int i = 0; i < matches.size(); i++) {
            GeminiWinAnalysisRequest.MatchItem match = matches.get(i);

            sb.append(i + 1).append("경기: ")
                    .append(Boolean.TRUE.equals(match.win()) ? "승리" : "패배")
                    .append(", 챔피언=").append(nullToDefault(match.championName(), "-"))
                    .append(", 포지션=").append(nullToDefault(match.position(), "-"))
                    .append(", KDA=")
                    .append(number(match.kills())).append("/")
                    .append(number(match.deaths())).append("/")
                    .append(number(match.assists()))
                    .append(", KDA지표=").append(number(match.kda()))
                    .append(", 딜량=").append(number(match.totalDamageDealtToChampions()))
                    .append(", CS=").append(number(match.totalCs()))
                    .append(", 골드=").append(number(match.goldEarned()))
                    .append(", 큐=").append(nullToDefault(match.queueType(), "-"))
                    .append(", 시간=").append(nullToDefault(match.gameDurationText(), "-"))
                    .append("\n");
        }

        return sb.toString();
    }

    private String extractGeminiText(Map body) {
        Object candidatesObject = body.get("candidates");

        if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini candidates가 비어 있습니다.");
        }

        Object firstCandidateObject = candidates.get(0);

        if (!(firstCandidateObject instanceof Map<?, ?> firstCandidate)) {
            throw new IllegalStateException("Gemini candidate 형식이 올바르지 않습니다.");
        }

        Object contentObject = firstCandidate.get("content");

        if (!(contentObject instanceof Map<?, ?> content)) {
            throw new IllegalStateException("Gemini content가 없습니다.");
        }

        Object partsObject = content.get("parts");

        if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
            throw new IllegalStateException("Gemini parts가 비어 있습니다.");
        }

        Object firstPartObject = parts.get(0);

        if (!(firstPartObject instanceof Map<?, ?> firstPart)) {
            throw new IllegalStateException("Gemini part 형식이 올바르지 않습니다.");
        }

        Object textObject = firstPart.get("text");

        if (textObject == null) {
            throw new IllegalStateException("Gemini text가 없습니다.");
        }

        return String.valueOf(textObject);
    }

    private GeminiTextResult parseGeminiJson(String text) throws Exception {
        String json = extractJson(text);
        JsonNode root = objectMapper.readTree(json);

        String summary = textValue(
                root,
                "summary",
                "최근 경기 데이터를 기반으로 승률 개선 리포트를 생성했습니다."
        );

        List<String> strengths = listValue(root, "strengths");
        List<String> weaknesses = listValue(root, "weaknesses");
        List<String> recommendations = listValue(root, "recommendations");

        if (strengths.isEmpty()) {
            throw new IllegalStateException("Gemini strengths 응답이 비어 있습니다.");
        }

        if (weaknesses.isEmpty()) {
            throw new IllegalStateException("Gemini weaknesses 응답이 비어 있습니다.");
        }

        if (recommendations.isEmpty()) {
            throw new IllegalStateException("Gemini recommendations 응답이 비어 있습니다.");
        }

        return new GeminiTextResult(summary, strengths, weaknesses, recommendations);
    }

    private GeminiTextResult buildFallbackTextResult(LocalStats stats) {
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (stats.winRate() >= 60) {
            strengths.add("최근 승률이 높아 전반적인 경기 흐름이 안정적입니다.");
        }

        if (stats.averageKda() >= 3.0) {
            strengths.add("평균 KDA가 높아 교전에서 손해를 크게 보지 않는 편입니다.");
        }

        if (stats.averageDamage() >= 25000) {
            strengths.add("평균 딜량이 높아 한타와 교전 기여도가 좋은 편입니다.");
        }

        if (stats.averageDeaths() <= 5.0) {
            strengths.add("평균 데스가 낮아 생존 관리가 안정적입니다.");
        }

        if (stats.averageCs() >= 170) {
            strengths.add("평균 CS가 준수해 성장 기반을 잘 유지하고 있습니다.");
        }

        if (strengths.isEmpty()) {
            strengths.add("최근 경기에서 뚜렷한 강점은 크지 않지만, 기본 지표를 바탕으로 개선 방향을 잡기 좋은 상태입니다.");
        }

        if (stats.winRate() < 50) {
            weaknesses.add("최근 승률이 낮아 초반 운영과 교전 선택을 점검할 필요가 있습니다.");
        }

        if (stats.averageDeaths() >= 7.0) {
            weaknesses.add("평균 데스가 높아 성장 손실과 오브젝트 손실로 이어질 가능성이 큽니다.");
        }

        if (stats.averageKda() < 2.0) {
            weaknesses.add("평균 KDA가 낮아 불리한 교전에서 손해를 보는 경우가 많을 수 있습니다.");
        }

        if (stats.averageCs() < 140) {
            weaknesses.add("평균 CS가 낮아 중후반 성장 차이가 벌어질 가능성이 있습니다.");
        }

        if (stats.averageDamage() < 15000) {
            weaknesses.add("평균 딜량이 낮아 한타 영향력이 부족할 수 있습니다.");
        }

        if (weaknesses.isEmpty()) {
            weaknesses.add("큰 약점은 보이지 않지만, 오브젝트 전 시야 확보와 데스 관리를 더 보완하면 좋습니다.");
        }

        if (stats.averageDeaths() >= 7.0) {
            recommendations.add("불리한 상황에서는 무리한 사이드 운영보다 데스를 줄이는 선택을 우선하세요.");
        }

        if (stats.averageCs() < 140) {
            recommendations.add("10분 CS 70개 이상, 20분 CS 150개 이상을 목표로 성장 안정성을 높이세요.");
        }

        if (stats.averageDamage() < 15000) {
            recommendations.add("한타 전 스킬을 먼저 소모하지 말고 핵심 딜 타이밍까지 포지션을 유지하세요.");
        }

        if (stats.winRate() < 50) {
            recommendations.add("패배 경기의 공통 원인을 줄이기 위해 초반 15분 데스 수를 먼저 관리하세요.");
        }

        recommendations.add("오브젝트 1분 전에는 제어 와드와 렌즈로 시야를 먼저 확보하세요.");

        String summary = buildFallbackSummary(stats);

        return new GeminiTextResult(summary, strengths, weaknesses, recommendations);
    }

    private String buildFallbackSummary(LocalStats stats) {
        if (stats.winRate() >= 60 && stats.averageKda() >= 3.0) {
            return "최근 경기 기준 승률과 KDA가 모두 안정적입니다. 현재 플레이 흐름은 좋은 편이며, 데스 관리와 오브젝트 시야를 유지하면 승률을 더 끌어올릴 수 있습니다.";
        }

        if (stats.winRate() <= 50 && stats.averageDeaths() >= 7.0) {
            return "최근 경기 기준 승률이 낮고 평균 데스가 높은 편입니다. 무리한 교전 선택을 줄이고, 불리한 상황에서는 생존과 시야 확보를 우선하는 운영이 필요합니다.";
        }

        if (stats.averageDamage() >= 25000 && stats.averageCs() < 140) {
            return "평균 딜량은 높아 교전 기여도는 좋지만, CS가 낮아 성장 안정성이 부족합니다. 라인 관리와 파밍 루틴을 보완하면 승률 개선 가능성이 큽니다.";
        }

        if (stats.averageKda() < 2.0) {
            return "최근 경기에서 KDA가 낮게 나타납니다. 확실한 이득이 없는 교전은 줄이고, 오브젝트 타이밍에 맞춘 시야 확보와 합류 중심 운영이 필요합니다.";
        }

        return "최근 경기 기준으로 전반적인 지표는 보통 수준입니다. 승률을 올리려면 데스 관리, CS 안정화, 오브젝트 전 시야 확보를 우선 개선하는 것이 좋습니다.";
    }

    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return text;
    }

    private String textValue(JsonNode root, String fieldName, String defaultValue) {
        JsonNode node = root.get(fieldName);

        if (node == null || node.isNull()) {
            return defaultValue;
        }

        String value = node.asText();

        return value == null || value.isBlank() ? defaultValue : value;
    }

    private List<String> listValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);

        if (node == null || !node.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();

        for (JsonNode item : node) {
            String value = item.asText();

            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }

        return result;
    }

    private LocalStats calculateLocalStats(
            GeminiWinAnalysisRequest request,
            List<GeminiWinAnalysisRequest.MatchItem> matches
    ) {
        int totalMatches = matches.size();
        int wins = 0;

        double totalKills = 0;
        double totalDeaths = 0;
        double totalAssists = 0;
        double totalKda = 0;
        double totalDamage = 0;
        double totalCs = 0;
        double totalGold = 0;

        for (GeminiWinAnalysisRequest.MatchItem match : matches) {
            if (Boolean.TRUE.equals(match.win())) {
                wins++;
            }

            double kills = number(match.kills());
            double deaths = number(match.deaths());
            double assists = number(match.assists());
            double kda = number(match.kda());

            totalKills += kills;
            totalDeaths += deaths;
            totalAssists += assists;
            totalKda += kda > 0 ? kda : (kills + assists) / Math.max(1, deaths);
            totalDamage += number(match.totalDamageDealtToChampions());
            totalCs += number(match.totalCs());
            totalGold += number(match.goldEarned());
        }

        int losses = totalMatches - wins;

        return new LocalStats(
                totalMatches,
                wins,
                losses,
                round1(wins * 100.0 / Math.max(1, totalMatches)),
                round2(totalKda / Math.max(1, totalMatches)),
                round1(totalKills / Math.max(1, totalMatches)),
                round1(totalDeaths / Math.max(1, totalMatches)),
                round1(totalAssists / Math.max(1, totalMatches)),
                round0(totalDamage / Math.max(1, totalMatches)),
                round0(totalCs / Math.max(1, totalMatches)),
                round0(totalGold / Math.max(1, totalMatches))
        );
    }

    private double number(Number value) {
        return value == null ? 0 : value.doubleValue();
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private double round0(double value) {
        return Math.round(value);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record GeminiTextResult(
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> recommendations
    ) {
    }

    private record LocalStats(
            int totalMatches,
            int wins,
            int losses,
            double winRate,
            double averageKda,
            double averageKills,
            double averageDeaths,
            double averageAssists,
            double averageDamage,
            double averageCs,
            double averageGold
    ) {
    }
}