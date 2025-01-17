package com.lokivpn.controller;

import com.lokivpn.model.Admin;
import com.lokivpn.model.PaymentRecord;
import com.lokivpn.model.User;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.AdminRepository;
import com.lokivpn.repository.PaymentRepository;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.repository.VpnClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private VpnClientRepository vpnClientRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

// Клиенты

    // Получение списка всех VPN-клиентов
    @GetMapping("/vpnclients")
    public ResponseEntity<List<VpnClient>> getAllVpnClients() {
        List<VpnClient> vpnClients = vpnClientRepository.findAll();
        return ResponseEntity.ok(vpnClients);
    }

    // Получение информации о VPN-клиенте по ID
    @GetMapping("/vpnclients/{id}")
    public ResponseEntity<List<VpnClient>> getVpnClientById(@PathVariable Long id) {
        List<VpnClient> vpnClients = vpnClientRepository.findByUserId(id);
        return ResponseEntity.ok(vpnClients);
    }

// Платежи

    // Получение всех платежей
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentRecord>> getAllPayments() {
        List<PaymentRecord> payments = paymentRepository.findAll();
        return ResponseEntity.ok(payments);
    }

    // Получение информации о платеже по ID
    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentRecord> getPaymentById(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

// Пользователи

    // Получение всех пользователей
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    // Получение пользователя по chat_id
    @GetMapping("/users/search")
    public ResponseEntity<User> getUserByChatId(@RequestParam Long chatId) {
        return userRepository.findByChatId(chatId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

// Добавить администратора

    @PostMapping("/add")
    public ResponseEntity<String> addAdmin(@RequestBody Admin admin) {
        try {
            if (adminRepository.findByUsername(admin.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists.");
            }
            adminRepository.save(admin);
            return ResponseEntity.ok("Admin added successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error adding admin: " + e.getMessage());
        }
    }

}
