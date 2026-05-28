package com.game.backend.repository;

import com.game.backend.entity.ChallengerRankingPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengerRankingPlayerRepository extends JpaRepository<ChallengerRankingPlayer, Long> {

    Optional<ChallengerRankingPlayer> findByPuuid(String puuid);

    List<ChallengerRankingPlayer> findByQueueTypeOrderByRankNoAsc(String queueType);
}