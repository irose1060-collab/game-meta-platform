package com.game.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seed_players",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_seed_player_riot_id",
                        columnNames = {"game_name", "tag_line"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeedPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_name", nullable = false, length = 100)
    private String gameName;

    @Column(name = "tag_line", nullable = false, length = 30)
    private String tagLine;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_collected_at")
    private LocalDateTime lastCollectedAt;

    @Column(name = "last_result_message", columnDefinition = "TEXT")
    private String lastResultMessage;

    @Column(name = "total_saved_matches", nullable = false)
    private int totalSavedMatches;

    @Column(name = "total_failed_count", nullable = false)
    private int totalFailedCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        enabled = true;
    }
}