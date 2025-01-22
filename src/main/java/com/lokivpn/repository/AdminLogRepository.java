package com.lokivpn.repository;

import com.lokivpn.model.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    // Фильтрация логов по администратору
    List<AdminLog> findByAdminId(Long adminId);

    // Фильтрация логов по пользователю
    List<AdminLog> findByUserId(Long userId);

    // Фильтрация логов по действию
    List<AdminLog> findByAction(String action);
}

