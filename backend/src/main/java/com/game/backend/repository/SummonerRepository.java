package com.game.backend.repository;

import com.game.backend.entity.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummonerRepository extends JpaRepository<Summoner, Long> {
    Optional<Summoner> findByPuuid(String puuid);
    Optional<Summoner> findByGameNameAndTagLine(String gameName, String tagLine);
}
