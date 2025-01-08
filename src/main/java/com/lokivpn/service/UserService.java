package com.lokivpn.service;

import com.lokivpn.model.User;
import com.lokivpn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void blockClient(Long id, LocalDateTime blockedUntil) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Клиент с ID " + id + " не найден"));
        user.setBlockedUntil(blockedUntil);
        userRepository.save(user);
    }
}
