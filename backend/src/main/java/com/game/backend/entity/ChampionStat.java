package com.game.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "champion_stats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_champion_stats_scope", columnNames = {"patch", "queue_id", "position", "champion_id"})
        },
        indexes = {
                @Index(name = "idx_champion_stats_position_tier", columnList = "position, tier"),
                @Index(name = "idx_champion_stats_score", columnList = "tier_score")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChampionStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String patch;

    @Column(name = "queue_id")
    private Integer queueId;

    @Column(length = 30)
    private String position;

    @Column(name = "champion_id")
    private Integer championId;

    @Column(name = "champion_name", length = 100)
    private String championName;

    private Integer games;
    private Integer wins;

    @Column(name = "win_rate")
    private Double winRate;

    @Column(name = "pick_rate")
    private Double pickRate;

    @Column(name = "avg_kda")
    private Double avgKda;

    @Column(name = "avg_damage")
    private Double avgDamage;

    @Column(name = "avg_gold")
    private Double avgGold;

    @Column(name = "avg_cs")
    private Double avgCs;

    @Column(name = "avg_vision_score")
    private Double avgVisionScore;

    @Column(name = "tier_score")
    private Double tierScore;

    @Column(length = 10)
    private String tier;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
