package com.lokivpn.repository;

import com.lokivpn.model.UserActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActionLogRepository extends JpaRepository<UserActionLog, Long> {
    List<UserActionLog> findByUserIdOrderByTimestampDesc(Long userId);
}
