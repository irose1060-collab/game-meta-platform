package com.game.backend.service;

import com.game.backend.dto.PatchNoteResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PatchNoteService {

    private static final String PATCH_LIST_URL =
            "https://www.leagueoflegends.com/ko-kr/news/tags/patch-notes/";

    private static final Pattern DATE_PATTERN =
            Pattern.compile("(20\\d{2}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z)");

    private static final Pattern PATCH_VERSION_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d+|\\d+\\.\\d+)\\s*패치");

    public List<PatchNoteResponse> getPatchNotes() {
        try {
            Document document = Jsoup.connect(PATCH_LIST_URL)
                    .userAgent("Mozilla/5.0 META-GG")
                    .timeout((int) Duration.ofSeconds(10).toMillis())
                    .get();

            List<PatchNoteResponse> notes = parsePatchNotes(document);

            if (notes.isEmpty()) {
                return fallbackNotes();
            }

            return notes;
        } catch (Exception e) {
            log.warn("Riot 공식 패치노트 조회 실패: {}", e.getMessage());
            return fallbackNotes();
        }
    }

    private List<PatchNoteResponse> parsePatchNotes(Document document) {
        List<PatchNoteResponse> result = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (Element link : document.select("a[href]")) {
            String href = link.attr("abs:href");

            if (href == null || href.isBlank()) {
                href = link.attr("href");
            }

            if (href == null || !href.contains("/ko-kr/news/game-updates/")) {
                continue;
            }

            String text = normalize(link.text());

            if (!text.contains("패치 노트")) {
                continue;
            }

            if (!seenUrls.add(href)) {
                continue;
            }

            String publishedAt = extractDate(text);
            String title = extractTitle(text);
            String patchVersion = extractPatchVersion(title);
            String summary = extractSummary(text, title);

            result.add(PatchNoteResponse.builder()
                    .title(title)
                    .patchVersion(patchVersion)
                    .category("게임 업데이트")
                    .publishedAt(publishedAt)
                    .summary(summary)
                    .officialUrl(href)
                    .build());
        }

        result.sort((a, b) -> nullSafe(b.getPublishedAt()).compareTo(nullSafe(a.getPublishedAt())));

        if (result.size() > 12) {
            return result.subList(0, 12);
        }

        return result;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\s+", " ").trim();
    }

    private String extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    private String extractTitle(String text) {
        String cleaned = text;

        cleaned = cleaned.replaceFirst("^게임 업데이트\\s*", "");
        cleaned = cleaned.replaceFirst(DATE_PATTERN.pattern(), "").trim();

        int titleEnd = cleaned.indexOf(" 패치 노트");
        if (titleEnd >= 0) {
            return cleaned.substring(0, titleEnd + " 패치 노트".length()).trim();
        }

        if (cleaned.length() > 70) {
            return cleaned.substring(0, 70).trim();
        }

        return cleaned.isBlank() ? "리그 오브 레전드 패치 노트" : cleaned;
    }

    private String extractPatchVersion(String title) {
        Matcher matcher = PATCH_VERSION_PATTERN.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    private String extractSummary(String text, String title) {
        String cleaned = text;

        cleaned = cleaned.replaceFirst("^게임 업데이트\\s*", "");
        cleaned = cleaned.replaceFirst(DATE_PATTERN.pattern(), "").trim();

        if (title != null && !title.isBlank()) {
            cleaned = cleaned.replace(title, "").trim();
        }

        if (cleaned.isBlank()) {
            return "Riot 공식 패치노트에서 상세 변경 내용을 확인할 수 있습니다.";
        }

        if (cleaned.length() > 180) {
            return cleaned.substring(0, 180).trim() + "...";
        }

        return cleaned;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private List<PatchNoteResponse> fallbackNotes() {
        return List.of(
                PatchNoteResponse.builder()
                        .title("Riot 공식 패치노트")
                        .patchVersion("")
                        .category("게임 업데이트")
                        .publishedAt("")
                        .summary("Riot 공식 패치노트 페이지에서 최신 업데이트 내용을 확인할 수 있습니다.")
                        .officialUrl(PATCH_LIST_URL)
                        .build()
        );
    }
}
