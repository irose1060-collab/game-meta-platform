package com.game.backend.repository;

import com.game.backend.entity.SeedPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeedPlayerRepository extends JpaRepository<SeedPlayer, Long> {

    List<SeedPlayer> findByEnabledTrueOrderByLastCollectedAtAsc();

    Optional<SeedPlayer> findByGameNameIgnoreCaseAndTagLineIgnoreCase(
            String gameName,
            String tagLine
    );
}