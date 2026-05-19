package com.game.backend.repository;

import com.game.backend.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByStatusOrderByIsPinnedDescCreatedAtDesc(String status);

    List<Notice> findTop3ByStatusOrderByIsPinnedDescCreatedAtDesc(String status);
}