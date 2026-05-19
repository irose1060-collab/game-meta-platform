package com.game.backend.repository;

import com.game.backend.entity.DataCollectionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataCollectionLogRepository extends JpaRepository<DataCollectionLog, Long> {
    List<DataCollectionLog> findTop10ByOrderByCreatedAtDesc();
}
