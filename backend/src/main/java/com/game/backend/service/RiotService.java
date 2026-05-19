package com.game.backend.service;

import com.game.backend.dto.RiotAccountResponse;
import com.game.backend.entity.Summoner;
import com.game.backend.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
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
        String encodedGameName = UriUtils.encodePathSegment(gameName, StandardCharsets.UTF_8);
        String encodedTagLine = UriUtils.encodePathSegment(tagLine, StandardCharsets.UTF_8);

        String url = baseUrl
                + "/riot/account/v1/accounts/by-riot-id/"
                + encodedGameName
                + "/"
                + encodedTagLine;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<RiotAccountResponse> response = restTemplate.exchange(
                    url,
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
        } catch (Exception e) {
            throw new RuntimeException("Riot API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
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
