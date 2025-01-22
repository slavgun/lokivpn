package com.lokivpn.service;

import com.lokivpn.model.Admin;
import com.lokivpn.model.AdminLog;
import com.lokivpn.repository.AdminLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminLogService {

    @Autowired
    private AdminLogRepository adminLogRepository;

    /**
     * Logs an admin action.
     *
     * @param admin  The admin performing the action.
     * @param action The type of action performed (e.g., "ADD_BALANCE").
     * @param userId The ID of the user the action was performed on (can be null if not applicable).
     * @param details Additional details about the action.
     */
    public void logAction(Admin admin, String action, Long userId, String details) {
        AdminLog log = new AdminLog();
        log.setAdmin(admin);
        log.setAction(action);
        log.setUserId(userId);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        adminLogRepository.save(log);
    }
}

