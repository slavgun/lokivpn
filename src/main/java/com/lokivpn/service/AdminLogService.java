package com.lokivpn.service;

import com.lokivpn.model.AdminLog;
import com.lokivpn.repository.AdminLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;

    @Autowired
    public AdminLogService(AdminLogRepository adminLogRepository) {
        this.adminLogRepository = adminLogRepository;
    }

    public void logAction(String adminUsername, String action, String details) {
        AdminLog log = new AdminLog();
        log.setAdminUsername(adminUsername);
        log.setAction(action);
        log.setDetails(details);
        log.setTimestamp(java.time.LocalDateTime.now());
        adminLogRepository.save(log);
    }
}
