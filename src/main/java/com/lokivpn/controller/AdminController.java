package com.lokivpn.controller;

import com.lokivpn.model.*;
import com.lokivpn.repository.*;
import com.lokivpn.service.AdminLogService;
import com.lokivpn.service.CustomAdminDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @Autowired
    private AdminLogRepository adminLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomAdminDetailsService customAdminDetailsService;

    @Autowired
    private AdminLogService adminLogService;

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
            admin.setPassword(passwordEncoder.encode(admin.getPassword())); // Хэшируем пароль
            adminRepository.save(admin);

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Администратор не найден"));
            adminLogService.logAction(currentAdmin, "ADD_ADMIN", null, "Добавлен администратор с именем: " + admin.getUsername());

            return ResponseEntity.ok("Admin added successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error adding admin: " + e.getMessage());
        }
    }

// Удалить администратора

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteAdmin(@PathVariable Long id) {
        try {
            Admin adminToDelete = adminRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Admin with ID " + id + " not found."));

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Current admin not found."));

            if (adminToDelete.getId().equals(currentAdmin.getId())) {
                return ResponseEntity.badRequest().body("You cannot delete yourself.");
            }

            adminLogService.logAction(currentAdmin, "DELETE_ADMIN", null, "Удален администратор с именем: " + adminToDelete.getUsername());
            adminRepository.deleteById(id);

            return ResponseEntity.ok("Admin deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting admin: " + e.getMessage());
        }
    }

    // Пополнить баланс пользователя
    @PostMapping("/users/{id}/add-balance")
    public ResponseEntity<String> addBalance(@PathVariable Long id, @RequestParam int amount) {
        return userRepository.findById(id).map(user -> {
            user.setBalance(user.getBalance() + amount);
            userRepository.save(user);

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Администратор не найден"));
            adminLogService.logAction(currentAdmin, "ADD_BALANCE", user.getId(), "Добавлено " + amount + " к балансу пользователя с ID " + user.getId());

            return ResponseEntity.ok("Balance added successfully.");
        }).orElse(ResponseEntity.status(404).body("User not found."));
    }

    // Вычесть сумму из баланса пользователя
    @PostMapping("/users/{id}/subtract-balance")
    public ResponseEntity<String> subtractBalance(@PathVariable Long id, @RequestParam int amount) {
        return userRepository.findById(id).map(user -> {
            if (user.getBalance() < amount) {
                return ResponseEntity.status(400).body("Insufficient balance.");
            }
            user.setBalance(user.getBalance() - amount);
            userRepository.save(user);

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Администратор не найден"));
            adminLogService.logAction(currentAdmin, "SUBTRACT_BALANCE", user.getId(), "Вычтено " + amount + " с баланса пользователя с ID " + user.getId());

            return ResponseEntity.ok("Balance subtracted successfully.");
        }).orElse(ResponseEntity.status(404).body("User not found."));
    }

    // Привязать клиента к пользователю
    @PostMapping("/users/{userId}/add-client/{clientId}")
    public ResponseEntity<String> addClientToUser(@PathVariable Long userId, @PathVariable Long clientId) {
        return userRepository.findById(userId).flatMap(user -> vpnClientRepository.findById(clientId).map(client -> {
            if (client.isAssigned()) {
                return ResponseEntity.status(400).body("Client is already assigned.");
            }
            client.setUserId(user.getId());
            client.setAssigned(true);
            vpnClientRepository.save(client);

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Администратор не найден"));
            adminLogService.logAction(currentAdmin, "ADD_CLIENT", user.getId(), "Привязан клиент с ID " + clientId + " к пользователю с ID " + userId);

            return ResponseEntity.ok("Client assigned to user successfully.");
        })).orElse(ResponseEntity.status(404).body("User or client not found."));
    }

    // Отвязать клиента от пользователя
    @PostMapping("/users/{userId}/remove-client/{clientId}")
    public ResponseEntity<String> removeClientFromUser(@PathVariable Long userId, @PathVariable Long clientId) {
        return vpnClientRepository.findById(clientId).map(client -> {
            if (!userId.equals(client.getUserId())) {
                return ResponseEntity.status(400).body("Client is not assigned to this user.");
            }
            client.setUserId(null);
            client.setAssigned(false);
            vpnClientRepository.save(client);

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Администратор не найден"));
            adminLogService.logAction(currentAdmin, "REMOVE_CLIENT", userId, "Отвязан клиент с ID " + clientId + " от пользователя с ID " + userId);

            return ResponseEntity.ok("Client removed from user successfully.");
        }).orElse(ResponseEntity.status(404).body("Client not found."));
    }

    // Забанить пользователя
    @PostMapping("/users/{id}/ban")
    public ResponseEntity<String> banUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setBanned(true);
            userRepository.save(user);

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Администратор не найден"));
            adminLogService.logAction(currentAdmin, "BAN_USER", user.getId(), "Заблокирован пользователь с ID " + user.getId());

            return ResponseEntity.ok("User banned successfully.");
        }).orElse(ResponseEntity.status(404).body("User not found."));
    }

    // Разбанить пользователя
    @PostMapping("/users/{id}/unban")
    public ResponseEntity<String> unbanUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setBanned(false);
            userRepository.save(user);

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Администратор не найден"));
            adminLogService.logAction(currentAdmin, "UNBAN_USER", user.getId(), "Разблокирован пользователь с ID " + user.getId());

            return ResponseEntity.ok("User unbanned successfully.");
        }).orElse(ResponseEntity.status(404).body("User not found."));
    }

    // Удалить пользователя из базы данных
    @DeleteMapping("/users/delete/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);

            Admin currentAdmin = adminRepository.findById(customAdminDetailsService.getCurrentAdminId())
                    .orElseThrow(() -> new RuntimeException("Администратор не найден"));
            adminLogService.logAction(currentAdmin, "DELETE_USER", id, "Удалён пользователь с ID " + id);


            return ResponseEntity.ok("User deleted successfully.");
        }
        return ResponseEntity.status(404).body("User not found.");
    }

    // Получение логов
    @GetMapping("/logs")
    public ResponseEntity<List<AdminLog>> getAllLogs() {
        List<AdminLog> logs = adminLogRepository.findAll();
        return ResponseEntity.ok(logs);
    }

}
