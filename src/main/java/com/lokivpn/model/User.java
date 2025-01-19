package com.lokivpn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column(name = "username")
    private String username;

    @Column(name = "balance", nullable = false)
    private int balance; // Баланс пользователя в рублях

    @Column(name = "referral_code")
    private String referralCode;

    @Column(name = "referred_by")
    private String referredBy;

    @Column(name = "referral_bonus")
    private double referralBonus;

    @Column(name = "referred_users_count")
    private int referredUsersCount;
}