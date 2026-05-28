package com.game.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionResultResponse {
    private String gameName;
    private String tagLine;
    private String puuid;
    private int requestedCount;
    private int receivedMatchIdCount;
    private int savedMatchCount;
    private int skippedExistingMatchCount;
    private int failedMatchCount;
    private int savedParticipantCount;
    private List<String> savedMatchIds;
    private List<String> failedMatchIds;
    private String message;
}
