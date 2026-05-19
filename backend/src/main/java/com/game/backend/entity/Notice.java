package com.game.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=200)
    private String title;

    @Column(nullable=false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name="is_pinned", nullable=false)
    private Boolean isPinned = false;

    @Builder.Default
    @Column(nullable=false, length=20)
    private String status = "VISIBLE";

    @Builder.Default
    @Column(name="view_count", nullable=false)
    private Integer viewCount = 0;

    @Column(name="created_by")
    private Long createdBy;

    @Column(name="updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name="created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;
}
