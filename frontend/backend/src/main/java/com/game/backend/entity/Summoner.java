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

    @Column(unique = true, nullable = false)
    private String puuid;

    private String gameName;
    private String tagLine;
    private Integer profileIconId;
    private Long summonerLevel;
    private String tier;
    private String rankDivision;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;

    private LocalDateTime lastFetchedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
