package com.game.backend.repository;

import com.game.backend.entity.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {
    long countByMatchId(String matchId);
    List<MatchParticipant> findByMatchIdOrderByTeamIdAscTeamPositionAsc(String matchId);
}
