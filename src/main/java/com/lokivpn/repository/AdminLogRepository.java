package com.lokivpn.repository;

import com.lokivpn.model.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    // Можно добавить методы сортировки, например:
    List<AdminLog> findAllByOrderByTimestampDesc();
    List<AdminLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate, LocalDateTime endDate);
    @Query("SELECT log FROM AdminLog log WHERE log.timestamp BETWEEN :start AND :end ORDER BY log.timestamp DESC")
    List<AdminLog> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

