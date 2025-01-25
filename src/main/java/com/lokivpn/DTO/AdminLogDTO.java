package com.lokivpn.DTO;

import com.lokivpn.model.AdminLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminLogDTO {
    private Long id;
    private String action;
    private Long userId;
    private String details;
    private LocalDateTime timestamp;
    private String adminUsername;

    public AdminLogDTO(AdminLog log) {
        this.id = log.getId();
        this.action = log.getAction();
        this.userId = log.getUserId();
        this.details = log.getDetails();
        this.timestamp = log.getTimestamp();
        this.adminUsername = log.getAdmin().getUsername();
    }
}
