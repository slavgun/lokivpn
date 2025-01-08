package com.lokivpn.controller;

import com.lokivpn.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/clients")
public class ClientController {

    private final UserService userService;

    @Autowired
    public ClientController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<?> blockClient(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String blockedUntilString = request.get("blockedUntil");
        if (blockedUntilString == null) {
            return ResponseEntity.badRequest().body("Дата блокировки отсутствует");
        }

        try {
            LocalDateTime blockedUntil = LocalDateTime.parse(blockedUntilString);
            userService.blockClient(id, blockedUntil);
            return ResponseEntity.ok("Клиент успешно заблокирован до " + blockedUntil);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка обработки запроса: " + e.getMessage());
        }
    }
}
