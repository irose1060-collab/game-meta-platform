package com.game.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_collection_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCollectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="job_name", length=100)
    private String jobName;

    @Column(length=20)
    private String status;

    @Column(name="started_at")
    private LocalDateTime startedAt;

    @Column(name="ended_at")
    private LocalDateTime endedAt;

    @Column(name="total_count")
    private Integer totalCount;

    @Column(name="success_count")
    private Integer successCount;

    @Column(name="fail_count")
    private Integer failCount;

    @Column(name="error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name="created_at")
    private LocalDateTime createdAt;
}
