package com.game.backend.repository;

import com.game.backend.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {
    boolean existsByMatchId(String matchId);
    Optional<MatchEntity> findByMatchId(String matchId);
}
