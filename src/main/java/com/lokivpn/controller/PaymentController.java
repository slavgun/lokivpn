package com.lokivpn.controller;

import com.lokivpn.service.PaymentService;
import com.lokivpn.service.VpnProvisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService; // Инъекция PaymentService

    @Autowired
    private VpnProvisionService vpnProvisionService;

    @PostMapping("/create")
    public ResponseEntity<String> createPaymentLink(@RequestParam Long userId, @RequestParam String username, @RequestParam String plan) {
        try {
            // Вызов метода сервиса для создания ссылки
            String paymentUrl = paymentService.createPaymentLink(userId, username, plan);
            return ResponseEntity.ok(paymentUrl);
        } catch (Exception e) {
            // Обработка ошибок
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating payment link");
        }
    }


    @PostMapping("/yookassa/webhook")
    public ResponseEntity<String> handleYooKassaWebhook(@RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> object = (Map<String, Object>) params.get("object");
            String status = (String) object.get("status");

            if ("succeeded".equals(status)) {
                Map<String, Object> metadata = (Map<String, Object>) object.get("metadata");
                String chatId = (String) metadata.get("chat_id");
                String username = (String) metadata.get("username");
                String plan = (String) metadata.get("plan");

                // Вызов assignVpnToDevice
                vpnProvisionService.assignVpnToDevice(chatId, username, plan);

                return ResponseEntity.ok("Webhook processed successfully");
            }

            return ResponseEntity.ok("Payment not yet completed");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
}
