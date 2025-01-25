package com.lokivpn.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDTO {
    private Long id;
    private String username;
    private Long userId;

    public AdminDTO(Long id, String username, Long userId) {
        this.id = id;
        this.username = username;
        this.userId = userId;
    }
    // Getters и Setters
}
