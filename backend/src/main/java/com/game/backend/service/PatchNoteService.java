package com.game.backend.service;

import com.game.backend.dto.PatchNoteResponse;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PatchNoteService {

    private static final String PATCH_NOTES_URL =
            "https://www.leagueoflegends.com/ko-kr/news/tags/patch-notes/";

    private static final String BASE_URL =
            "https://www.leagueoflegends.com";

    public List<PatchNoteResponse> getPatchNotes() {
        try {
            Document document = Jsoup.connect(PATCH_NOTES_URL)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            return parsePatchNotes(document);
        } catch (Exception e) {
            return fallbackPatchNotes();
        }
    }

    private List<PatchNoteResponse> parsePatchNotes(Document document) {
        List<PatchNoteResponse> result = new ArrayList<>();
        Set<String> usedUrls = new LinkedHashSet<>();

        Elements links = document.select("a[href*=/news/game-updates/]");

        for (Element link : links) {
            String href = link.attr("href");

            if (href == null || href.isBlank()) {
                continue;
            }

            String url = normalizeUrl(href);

            if (!url.contains("/news/game-updates/")) {
                continue;
            }

            String rawText = link.text().trim();

            if (!isPatchNoteText(rawText) && !url.contains("patch")) {
                continue;
            }

            if (usedUrls.contains(url)) {
                continue;
            }

            usedUrls.add(url);

            String title = extractTitle(rawText, url);
            String date = extractDate(rawText);
            String description = extractDescription(rawText, title, date);
            String slug = extractSlug(url);

            result.add(
                    PatchNoteResponse.builder()
                            .title(title)
                            .date(date)
                            .description(description)
                            .url(url)
                            .slug(slug)
                            .build()
            );

            if (result.size() >= 20) {
                break;
            }
        }

        if (result.isEmpty()) {
            return fallbackPatchNotes();
        }

        return result;
    }

    private boolean isPatchNoteText(String text) {
        if (text == null) return false;

        String lower = text.toLowerCase();

        return lower.contains("패치")
                || lower.contains("patch")
                || lower.contains("patch notes");
    }

    private String extractTitle(String text, String url) {
        if (text == null || text.isBlank()) {
            return extractSlug(url);
        }

        String cleaned = text
                .replaceAll("\\s+", " ")
                .trim();

        int index = cleaned.indexOf("리그 오브 레전드");
        if (index >= 0) {
            return cleaned.substring(index).trim();
        }

        index = cleaned.toLowerCase().indexOf("league of legends");
        if (index >= 0) {
            return cleaned.substring(index).trim();
        }

        if (cleaned.length() > 80) {
            return cleaned.substring(0, 80) + "...";
        }

        return cleaned;
    }

    private String extractDate(String text) {
        if (text == null) return "-";

        // 예: 2026-05-12T18:00:00.000Z
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})");

        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "-";
    }

    private String extractDescription(String text, String title, String date) {
        if (text == null || text.isBlank()) {
            return "공식 패치노트 상세 내용을 확인하세요.";
        }

        String cleaned = text
                .replace(title, "")
                .replace(date, "")
                .replace("게임 업데이트", "")
                .replace("Game Updates", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isBlank()) {
            return "공식 패치노트 상세 내용을 확인하세요.";
        }

        if (cleaned.length() > 120) {
            return cleaned.substring(0, 120) + "...";
        }

        return cleaned;
    }

    private String normalizeUrl(String href) {
        if (href.startsWith("http")) {
            return href;
        }

        if (href.startsWith("/")) {
            return BASE_URL + href;
        }

        return BASE_URL + "/" + href;
    }

    private String extractSlug(String url) {
        if (url == null || url.isBlank()) return "";

        String cleaned = url;

        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        int index = cleaned.lastIndexOf("/");

        if (index >= 0 && index < cleaned.length() - 1) {
            return cleaned.substring(index + 1);
        }

        return cleaned;
    }

    private List<PatchNoteResponse> fallbackPatchNotes() {
        return List.of(
                PatchNoteResponse.builder()
                        .title("리그 오브 레전드 26.10 패치 노트")
                        .date("2026-05-12")
                        .description("악마의 시즌이 26.10 패치와 함께 계속됩니다.")
                        .url("https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-10-notes/")
                        .slug("league-of-legends-patch-26-10-notes")
                        .build(),
                PatchNoteResponse.builder()
                        .title("리그 오브 레전드 26.9 패치 노트")
                        .date("2026-04-28")
                        .description("26.9 패치와 함께 악마 사냥을 시작하세요.")
                        .url("https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-9-notes/")
                        .slug("league-of-legends-patch-26-9-notes")
                        .build(),
                PatchNoteResponse.builder()
                        .title("리그 오브 레전드 26.8 패치 노트")
                        .date("2026-04-14")
                        .description("그루브가 넘치는 26.8 패치입니다.")
                        .url("https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-8-notes/")
                        .slug("league-of-legends-patch-26-8-notes")
                        .build()
        );
    }
}