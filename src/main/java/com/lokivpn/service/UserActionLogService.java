package com.lokivpn.service;

import com.lokivpn.model.UserActionLog;
import com.lokivpn.repository.UserActionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserActionLogService {
    @Autowired
    private UserActionLogRepository logRepository;

    public void logAction(Long userId, String actionType, String details) {
        UserActionLog log = new UserActionLog();
        log.setUserId(userId);
        log.setActionType(actionType);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
    }

    public List<UserActionLog> getLogsForUser(Long userId) {
        return logRepository.findByUserIdOrderByTimestampDesc(userId);
    }
}

