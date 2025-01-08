package com.lokivpn.model;

import jakarta.persistence.*;

@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Уникальный идентификатор

    @Column(nullable = false, unique = true)
    private String username; // Уникальное имя пользователя

    @Column(nullable = false)
    private String password; // Хэшированный пароль

    // Конструктор без аргументов (обязателен для JPA)
    public Admin() {
    }

    // Конструктор с параметрами
    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters и Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
