package com.game.backend.service;

import com.game.backend.dto.RiotAccountResponse;
import com.game.backend.entity.Summoner;
import com.game.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiotService {

    private final SummonerRepository summonerRepository;
    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    @Value("${riot.account.base-url}")
    private String baseUrl;

    @Transactional
    public RiotAccountResponse getAccountByRiotId(String gameName, String tagLine) {
        String cleanGameName = normalizeRequired(gameName, "gameName");
        String cleanTagLine = normalizeRequired(tagLine, "tagLine");

        URI uri = UriComponentsBuilder
                .fromHttpUrl(normalizeBaseUrl(baseUrl))
                .pathSegment(
                        "riot",
                        "account",
                        "v1",
                        "accounts",
                        "by-riot-id",
                        cleanGameName,
                        cleanTagLine
                )
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey == null ? "" : riotApiKey.trim());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<RiotAccountResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    RiotAccountResponse.class
            );

            RiotAccountResponse account = response.getBody();

            if (account == null || account.getPuuid() == null) {
                throw new RuntimeException("Riot 계정 정보를 찾을 수 없습니다.");
            }

            saveOrUpdateSummoner(account);

            return account;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RuntimeException(
                        "Riot 계정을 찾을 수 없습니다. 입력한 Riot ID를 확인하세요: "
                                + cleanGameName
                                + "#"
                                + cleanTagLine
                );
            }

            throw new RuntimeException(
                    "Riot API 호출 중 오류가 발생했습니다. status="
                            + e.getStatusCode()
                            + ", body="
                            + e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            throw new RuntimeException("Riot API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(fieldName + " 값이 비어 있습니다.");
        }

        return value.trim();
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalStateException("riot.account.base-url 설정값이 비어 있습니다.");
        }

        String clean = value.trim();

        if (clean.endsWith("/")) {
            return clean.substring(0, clean.length() - 1);
        }

        return clean;
    }

    private void saveOrUpdateSummoner(RiotAccountResponse account) {
        Summoner summoner = summonerRepository.findByPuuid(account.getPuuid())
                .orElseGet(() -> Summoner.builder()
                        .puuid(account.getPuuid())
                        .createdAt(LocalDateTime.now())
                        .build());

        summoner.setGameName(account.getGameName());
        summoner.setTagLine(account.getTagLine());
        summoner.setLastFetchedAt(LocalDateTime.now());

        summonerRepository.save(summoner);
    }
}