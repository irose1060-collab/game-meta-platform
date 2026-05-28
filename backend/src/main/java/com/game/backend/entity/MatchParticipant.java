package com.game.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "match_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_match_participant_match_puuid", columnNames = {"match_id", "puuid"})
        },
        indexes = {
                @Index(name = "idx_participants_champion_position", columnList = "champion_id, team_position"),
                @Index(name = "idx_participants_puuid", columnList = "puuid"),
                @Index(name = "idx_participants_match_id", columnList = "match_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, length = 100)
    private String matchId;

    @Column(name = "puuid", nullable = false, columnDefinition = "TEXT")
    private String puuid;

    @Column(name = "riot_game_name", length = 100)
    private String riotGameName;

    @Column(name = "riot_tag_line", length = 30)
    private String riotTagLine;

    @Column(name = "summoner_name", length = 100)
    private String summonerName;

    @Column(name = "champion_id")
    private Integer championId;

    @Column(name = "champion_name", length = 100)
    private String championName;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "team_position", length = 30)
    private String teamPosition;

    @Column(name = "individual_position", length = 30)
    private String individualPosition;

    private Boolean win;
    private Integer kills;
    private Integer deaths;
    private Integer assists;

    @Column(name = "total_damage_dealt_to_champions")
    private Integer totalDamageDealtToChampions;

    @Column(name = "total_damage_taken")
    private Integer totalDamageTaken;

    @Column(name = "gold_earned")
    private Integer goldEarned;

    @Column(name = "total_minions_killed")
    private Integer totalMinionsKilled;

    @Column(name = "neutral_minions_killed")
    private Integer neutralMinionsKilled;

    @Column(name = "vision_score")
    private Integer visionScore;

    @Column(name = "wards_placed")
    private Integer wardsPlaced;

    @Column(name = "wards_killed")
    private Integer wardsKilled;

    @Column(name = "summoner1_id")
    private Integer summoner1Id;

    @Column(name = "summoner2_id")
    private Integer summoner2Id;

    private Integer item0;
    private Integer item1;
    private Integer item2;
    private Integer item3;
    private Integer item4;
    private Integer item5;
    private Integer item6;

    @Column(name = "primary_style_id")
    private Integer primaryStyleId;

    @Column(name = "sub_style_id")
    private Integer subStyleId;

    @Column(name = "main_rune_id")
    private Integer mainRuneId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
