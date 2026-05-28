package com.game.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "challenger_ranking_players",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_challenger_ranking_puuid", columnNames = {"puuid"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengerRankingPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rank_no", nullable = false)
    private int rankNo;

    @Column(name = "queue_type", nullable = false, length = 50)
    private String queueType;

    @Column(name = "puuid", nullable = false, columnDefinition = "TEXT")
    private String puuid;

    @Column(name = "game_name", length = 100)
    private String gameName;

    @Column(name = "tag_line", length = 30)
    private String tagLine;

    @Column(name = "league_points")
    private int leaguePoints;

    @Column(name = "wins")
    private int wins;

    @Column(name = "losses")
    private int losses;

    @Column(name = "win_rate")
    private double winRate;

    @Column(name = "seed_added", nullable = false)
    private boolean seedAdded;

    @Column(name = "last_refreshed_at", nullable = false)
    private LocalDateTime lastRefreshedAt;
}