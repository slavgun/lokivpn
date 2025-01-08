package com.lokivpn.service;

import com.lokivpn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;

    @Autowired
    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin(String chatId) {
        return userRepository.findByChatId(chatId)
                .map(user -> "ADMIN".equals(user.getRole()))
                .orElse(false);
    }
}


