package com.game.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "summoners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Summoner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 120)
    private String puuid;

    @Column(name = "game_name", length = 80)
    private String gameName;

    @Column(name = "tag_line", length = 20)
    private String tagLine;

    @Column(name = "profile_icon_id")
    private Integer profileIconId;

    @Column(name = "summoner_level")
    private Long summonerLevel;

    private String tier;

    @Column(name = "rank_division")
    private String rankDivision;

    @Column(name = "league_points")
    private Integer leaguePoints;

    private Integer wins;
    private Integer losses;

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
