package com.lokivpn.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String chatId;
    private String role; // ADMIN, USER, CLIENT
    private String plan;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime blockedUntil;

    @Column(name = "last_plan_selected")
    private String lastPlanSelected;

    @Column(name = "last_plan_time")
    private Instant lastPlanTime;


    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(LocalDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public String getLastPlanSelected() {
        return lastPlanSelected;
    }

    public void setLastPlanSelected(String lastPlanSelected) {
        this.lastPlanSelected = lastPlanSelected;
    }

    public Instant getLastPlanTime() {
        return lastPlanTime;
    }

    public void setLastPlanTime(Instant lastPlanTime) {
        this.lastPlanTime = lastPlanTime;
    }

}

