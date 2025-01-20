package com.lokivpn.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class UserActionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String actionType; // Тип действия (Пополнение, Создание конфига и т.д.)
    private String details; // Дополнительная информация (например, сумма или причина)
    private LocalDateTime timestamp; // Время действия

    // Геттеры и сеттеры
}
