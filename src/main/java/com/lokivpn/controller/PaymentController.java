package com.lokivpn.controller;

import com.lokivpn.bot.TelegramBotService;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.VpnClientRepository;
import com.lokivpn.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService; // Инъекция PaymentService

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private VpnClientRepository vpnClientRepository;

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

            if (!"succeeded".equals(status)) {
                return ResponseEntity.ok("Payment not completed");
            }

            Map<String, Object> metadata = (Map<String, Object>) object.get("metadata");
            String chatId = (String) metadata.get("chat_id");
            String plan = (String) metadata.get("plan");

            // Находим клиента с активной резервацией
            VpnClient client = vpnClientRepository.findFirstByChatIdAndPlanAndReservedUntilAfter(chatId, plan, LocalDateTime.now())
                    .orElseThrow(() -> new RuntimeException("Клиент не найден для пользователя: " + chatId));

            // Устанавливаем срок действия подписки
            LocalDateTime expirationDate = switch (plan) {
                case "1_month" -> LocalDateTime.now().plusMonths(1);
                case "3_months" -> LocalDateTime.now().plusMonths(3);
                case "6_months" -> LocalDateTime.now().plusMonths(6);
                case "1_year" -> LocalDateTime.now().plusYears(1);
                default -> throw new IllegalArgumentException("Invalid plan: " + plan);
            };
            client.setReservedUntil(null); // Убираем резервацию
            client.setExpirationDate(expirationDate);
            vpnClientRepository.save(client);

            logger.info("Оплата успешно завершена для клиента {}. Подписка активна до {}.", client.getId(), expirationDate);

            // Уведомляем пользователя
            SendMessage message = new SendMessage(chatId, "Подписка успешно активирована. Дата окончания: " + expirationDate);
            telegramBotService.getBot().execute(message);

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            logger.error("Ошибка при обработке webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
}
