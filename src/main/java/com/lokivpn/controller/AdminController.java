package com.lokivpn.controller;

import com.lokivpn.model.*;
import com.lokivpn.repository.*;
import com.lokivpn.service.AdminLogService;
import com.lokivpn.service.AdminService;
import com.lokivpn.service.TelegramNotificationService;
import com.lokivpn.service.VpnProvisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminLogRepository adminLogRepository;
    private final VpnProvisionService vpnProvisionService;
    private final VpnClientRepository vpnClientRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final AdminLogService adminLogService;
    private final UserRepository userRepository;

    @Autowired
    public AdminController(VpnProvisionService vpnProvisionService,
                           VpnClientRepository vpnClientRepository,
                           AdminLogRepository adminLogRepository,
                           TelegramNotificationService telegramNotificationService,
                           AdminLogService adminLogService,
                           UserRepository userRepository) {

        this.vpnProvisionService = vpnProvisionService;
        this.vpnClientRepository = vpnClientRepository;
        this.adminLogRepository = adminLogRepository;
        this.telegramNotificationService = telegramNotificationService;
        this.adminLogService = adminLogService;
        this.userRepository = userRepository;
    }

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private AdminService adminService;

    @GetMapping("/appusers")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Получение списка серверов
    @GetMapping("/servers")
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    @PostMapping("/notify")
    public ResponseEntity<String> sendNotification(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Пустое сообщение!");
        }

        String adminUsername = "admin"; // Здесь нужно получить текущего администратора

        // Логирование
        adminLogService.logAction(adminUsername, "SEND_NOTIFICATION", "Message: " + message);

        telegramNotificationService.sendNotification(adminUsername, message);
        return ResponseEntity.ok("Уведомление отправлено");
    }

    @PatchMapping("/vpnclients/{id}/release")
    public ResponseEntity<String> releaseVpnClient(@PathVariable Long id) {
        Optional<VpnClient> vpnClientOptional = vpnClientRepository.findById(id);
        if (vpnClientOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("VPN client not found");
        }

        VpnClient vpnClient = vpnClientOptional.get();
        vpnClient.setChatId(null);
        vpnClient.setAssignedAt(null);
        vpnClient.setUsername(null);
        vpnClientRepository.save(vpnClient);

        String adminUsername = "admin"; // Здесь нужно получить текущего администратора

        // Логирование
        adminLogService.logAction(adminUsername, "RELEASE_CLIENT", "Released VPN Client ID: " + id);

        return ResponseEntity.ok("VPN client released successfully");
    }


    // Добавление сервера
    @PostMapping("/servers")
    public ResponseEntity<String> addServer(@RequestBody Server server) {
        server.setStatus("Active"); // Статус по умолчанию
        serverRepository.save(server);
        return ResponseEntity.ok("Сервер добавлен.");
    }

    // Удаление сервера
    @DeleteMapping("/servers/{id}")
    public ResponseEntity<String> deleteServer(@PathVariable Long id) {
        if (!serverRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Сервер не найден.");
        }
        serverRepository.deleteById(id);
        return ResponseEntity.ok("Сервер удалён.");
    }

    // Изменение сервера
    @PutMapping("/servers/{id}")
    public ResponseEntity<String> updateServer(@PathVariable Long id, @RequestBody Server updatedServer) {
        Optional<Server> serverOptional = serverRepository.findById(id);
        if (serverOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Сервер не найден.");
        }
        Server server = serverOptional.get();
        server.setName(updatedServer.getName());
        server.setIpAddress(updatedServer.getIpAddress());
        server.setStatus(updatedServer.getStatus());
        serverRepository.save(server);
        return ResponseEntity.ok("Сервер обновлён.");
    }

    // Получение списка VPN-клиентов
    @GetMapping("/vpnclients")
    public List<VpnClient> getAllVpnClients() {
        return vpnClientRepository.findAll();
    }

    @GetMapping("/logs")
    public List<AdminLog> getAllLogs() {
        // Можно вернуть все или ограничить количество (например, последние 100 записей)
        return adminLogRepository.findAllByOrderByTimestampDesc();
    }

    @GetMapping("/reports/stats")
    public ResponseEntity<Map<String, Object>> generateStatsReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalClients", vpnClientRepository.count());
        report.put("assignedClients", vpnClientRepository.countByIsAssigned(true));
        report.put("freeClients", vpnClientRepository.countByIsAssigned(false));
        report.put("pcClients", vpnClientRepository.countByDeviceType("PC"));
        report.put("phoneClients", vpnClientRepository.countByDeviceType("Phone"));
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/logs")
    public ResponseEntity<List<AdminLog>> generateLogsReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AdminLog> logs = adminLogRepository.findByTimestampBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        return ResponseEntity.ok(logs);
    }

    // Получение статистики
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        // Общее количество
        stats.put("totalClients", vpnClientRepository.count());

        // Количество занятых
        int assignedCount = vpnClientRepository.countByIsAssigned(true);
        stats.put("assignedCount", assignedCount);

        // Количество свободных
        int freeCount = vpnClientRepository.countByIsAssigned(false);
        stats.put("freeCount", freeCount);

        // Количество PC
        int pcCount = vpnClientRepository.countByDeviceType("PC");
        stats.put("pcCount", pcCount);

        // Количество Phone
        int phoneCount = vpnClientRepository.countByDeviceType("Phone");
        stats.put("phoneCount", phoneCount);

        // Возвращаем для фронтенда
        return stats;
    }

    @PostMapping("/notifyAllUsers")
    public ResponseEntity<String> notifyAllUsers(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Пустое сообщение!");
        }

        // Получаем всех пользователей
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getChatId() != null) {
                // Шлём уведомление
                telegramNotificationService.sendNotification(user.getChatId(), message);
            }
        }
        return ResponseEntity.ok("Уведомление отправлено всем пользователям.");
    }

    @GetMapping("/logs/filter")
    public List<AdminLog> getLogsByDateRange(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        if (startDate == null && endDate == null) {
            return adminLogRepository.findAllByOrderByTimestampDesc(); // Без фильтров
        }
        return adminLogRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
    }

    @PatchMapping("/vpnclients/{id}/reserve")
    public ResponseEntity<String> reserveVpnClient(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload
    ) {
        Optional<VpnClient> vpnClientOptional = vpnClientRepository.findById(id);
        if (vpnClientOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("VPN client not found");
        }

        VpnClient vpnClient = vpnClientOptional.get();
        String reserveUntil = payload.get("reservedUntil");

        try {
            LocalDateTime reservedUntil = LocalDateTime.parse(reserveUntil);
            vpnClient.setReservedUntil(reservedUntil);
            vpnClientRepository.save(vpnClient);

            return ResponseEntity.ok("VPN client successfully reserved until " + reservedUntil);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid date format");
        }
    }

    @PostMapping("/notifyAdmins")
    public ResponseEntity<String> notifyAdmins(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Пустое сообщение!");
        }

        // Находим всех, у кого role = 'ADMIN'
        List<User> admins = userRepository.findByRole("ADMIN");
        for (User admin : admins) {
            if (admin.getChatId() != null) {
                telegramNotificationService.sendNotification(admin.getChatId(), message);
            }
        }
        return ResponseEntity.ok("Уведомление отправлено всем админам.");
    }



    @PostMapping("/notifyUser/{username}")
    public ResponseEntity<String> notifyUser(@PathVariable String username,
                                             @RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Пустое сообщение!");
        }

        // Находим конкретного пользователя
        User user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь не найден: " + username);
        }
        if (user.getChatId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("У пользователя нет chatId");
        }

        telegramNotificationService.sendNotification(user.getChatId(), message);

        return ResponseEntity.ok("Уведомление отправлено пользователю " + username);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        // Логирование ошибки для отладки
        System.err.println("Ошибка: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка: " + e.getMessage());
    }

    @GetMapping("/vpnclients/sorted")
    public List<VpnClient> getVpnClients(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) Boolean isAssigned) {
        Specification<VpnClient> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (username != null) {
                predicates.add(criteriaBuilder.like(root.get("username"), "%" + username + "%"));
            }
            if (deviceType != null) {
                predicates.add(criteriaBuilder.equal(root.get("deviceType"), deviceType));
            }
            if (isAssigned != null) {
                predicates.add(criteriaBuilder.equal(root.get("isAssigned"), isAssigned));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return vpnClientRepository.findAll(specification);
    }


    @PatchMapping("/users/bulkRelease")
    public ResponseEntity<String> bulkRelease(@RequestBody Map<String, List<Long>> payload) {
        List<Long> ids = payload.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("Список ID пуст!");
        }

        List<VpnClient> clients = vpnClientRepository.findAllById(ids);
        for (VpnClient client : clients) {
            client.setChatId(null);
            client.setAssignedAt(null);
            client.setUsername(null);
        }
        vpnClientRepository.saveAll(clients);

        // Логируем
        AdminLog log = new AdminLog();
        log.setAdminUsername("admin"); // Если есть логика определения админа
        log.setAction("BULK_RELEASE");
        log.setDetails("Освобождены ID: " + ids);
        adminLogRepository.save(log);

        return ResponseEntity.ok("Клиенты успешно освобождены");
    }


    @GetMapping("/vpnclients/search")
    public List<VpnClient> searchVpnClients(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "deviceType", required = false) String deviceType,
            @RequestParam(value = "isAssigned", required = false) Boolean isAssigned
    ) {
        if (username != null || deviceType != null || isAssigned != null) {
            return vpnClientRepository.findByFilters(username, deviceType, isAssigned);
        }
        return vpnClientRepository.findAllByOrderByIdAsc(); // Если фильтры не заданы, возвращаем всех клиентов
    }
}