package com.game.backend.repository;

import com.game.backend.entity.ChampionStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionStatRepository extends JpaRepository<ChampionStat, Long> {
    List<ChampionStat> findByPositionOrderByTierScoreDesc(String position);
    List<ChampionStat> findByPatchAndPositionOrderByTierScoreDesc(String patch, String position);
}
