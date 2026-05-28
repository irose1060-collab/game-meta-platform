package com.game.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "matches",
        indexes = {
                @Index(name = "idx_matches_queue_version", columnList = "queue_id, game_version"),
                @Index(name = "idx_matches_game_creation", columnList = "game_creation")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, unique = true, length = 100)
    private String matchId;

    @Column(name = "game_version", length = 50)
    private String gameVersion;

    @Column(name = "queue_id")
    private Integer queueId;

    @Column(name = "game_creation")
    private Long gameCreation;

    @Column(name = "game_duration")
    private Integer gameDuration;

    @Column(name = "platform_id", length = 30)
    private String platformId;

    @Column(name = "winning_team_id")
    private Integer winningTeamId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
