package com.lokivpn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "admin_logs")
public class AdminLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // Связь "многие к одному"
    @JoinColumn(name = "admin_id", nullable = false) // Указывает на `id` в таблице admins
    private Admin admin; // Объект администратора

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "user_id")
    private Long userId; // ID пользователя, к которому было применено действие

    @Column(name = "details")
    private String details; // Дополнительная информация

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}


