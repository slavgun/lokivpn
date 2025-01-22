package com.lokivpn.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "secure_password"; // Укажите ваш пароль здесь
        String hashedPassword = passwordEncoder.encode(rawPassword);
        System.out.println("Hashed password: " + hashedPassword);
    }
}

