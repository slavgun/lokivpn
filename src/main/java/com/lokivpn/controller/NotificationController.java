package com.lokivpn.controller;

import com.lokivpn.DTO.NotificationRequest;
import com.lokivpn.model.Admin;
import com.lokivpn.model.User;
import com.lokivpn.repository.AdminRepository;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.service.TelegramMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TelegramMessageSender messageSender;

    // Отправка уведомлений всем пользователям
    @PostMapping("/users")
    public ResponseEntity<String> notifyAllUsers(@RequestBody NotificationRequest request) {
        List<User> users = userRepository.findAll();
        users.forEach(user -> messageSender.sendCustomNotification(
                user.getChatId(),
                request.getMessage(),
                request.getPhotoUrl(),
                request.getButtonTexts(),
                request.getButtonUrls()
        ));
        return ResponseEntity.ok("Notifications sent to all users.");
    }

    // Отправка уведомлений администраторам
    @PostMapping("/admins")
    public ResponseEntity<String> notifyAdmins(@RequestBody NotificationRequest request) {
        List<Admin> admins = adminRepository.findAll();
        admins.forEach(admin -> messageSender.sendCustomNotification(
                admin.getUser_id(),
                request.getMessage(),
                request.getPhotoUrl(),
                request.getButtonTexts(),
                request.getButtonUrls()
        ));
        return ResponseEntity.ok("Notifications sent to all admins.");
    }

    // Отправка уведомлений пользователям с балансом > 0
    @PostMapping("/users/with-balance")
    public ResponseEntity<String> notifyUsersWithBalance(@RequestBody NotificationRequest request) {
        List<User> users = userRepository.findAll();
        users.stream()
                .filter(user -> user.getBalance() > 0)
                .forEach(user -> messageSender.sendCustomNotification(
                        user.getChatId(),
                        request.getMessage(),
                        request.getPhotoUrl(),
                        request.getButtonTexts(),
                        request.getButtonUrls()
                ));
        return ResponseEntity.ok("Notifications sent to users with balance.");
    }

    // Отправка уведомлений пользователям с балансом = 0
    @PostMapping("/users/no-balance")
    public ResponseEntity<String> notifyUsersWithoutBalance(@RequestBody NotificationRequest request) {
        List<User> users = userRepository.findAll();
        users.stream()
                .filter(user -> user.getBalance() == 0)
                .forEach(user -> messageSender.sendCustomNotification(
                        user.getChatId(),
                        request.getMessage(),
                        request.getPhotoUrl(),
                        request.getButtonTexts(),
                        request.getButtonUrls()
                ));
        return ResponseEntity.ok("Notifications sent to users without balance.");
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendCustomNotification(@RequestBody NotificationRequest request) {
        try {
            // Заполнение значений по умолчанию, если они не переданы
            String message = request.getMessage() != null ? request.getMessage() : "Default message";
            String photoUrl = request.getPhotoUrl();
            List<String> buttonTexts = request.getButtonTexts() != null ? request.getButtonTexts() : new ArrayList<>();
            List<String> buttonUrls = request.getButtonUrls() != null ? request.getButtonUrls() : new ArrayList<>();

            // Вызов сервиса для отправки уведомления
            messageSender.sendCustomNotification(
                    request.getChatId(),
                    message,
                    photoUrl,
                    buttonTexts,
                    buttonUrls
            );

            return ResponseEntity.ok("Notification sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send notification: " + e.getMessage());
        }
    }
}

