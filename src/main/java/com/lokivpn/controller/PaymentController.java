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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

            if ("succeeded".equals(status)) {
                Map<String, Object> metadata = (Map<String, Object>) object.get("metadata");
                String chatId = (String) metadata.get("chat_id");
                String username = (String) metadata.get("username");
                String plan = (String) metadata.get("plan");

                // Назначение VPN клиенту
                VpnClient vpnClient = telegramBotService.assignClientToUser(chatId, username, "Phone");

                // Установка срока подписки
                LocalDateTime reservedUntil = switch (plan) {
                    case "1 месяц" -> LocalDateTime.now().plusMonths(1);
                    case "3 месяца" -> LocalDateTime.now().plusMonths(3);
                    case "6 месяцев" -> LocalDateTime.now().plusMonths(6);
                    case "1 год" -> LocalDateTime.now().plusYears(1);
                    default -> throw new IllegalArgumentException("Invalid plan: " + plan);
                };
                vpnClient.setReservedUntil(reservedUntil);
                vpnClient.setAssigned(true);

                vpnClientRepository.save(vpnClient);

                // Отправка сообщения пользователю
                SendMessage message = new SendMessage();
                message.setChatId(chatId);

                message.setText("Поздравляем! Ваш VPN успешно активирован:\n\n"
                        + "Устройство: " + vpnClient.getDeviceType() + "\n"
                        + "План: " + plan + "\n"
                        + "Действует до: " + vpnClient.getReservedUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

                // Кнопка для скачивания конфигурации
                InlineKeyboardButton configButton = new InlineKeyboardButton("📂 Скачать конфиг");
                configButton.setCallbackData("download_config_" + vpnClient.getId());
                buttons.add(Collections.singletonList(configButton));

                // Кнопка для отображения QR-кода
                InlineKeyboardButton qrButton = new InlineKeyboardButton("📷 Показать QR-код");
                qrButton.setCallbackData("show_qr_" + vpnClient.getId());
                buttons.add(Collections.singletonList(qrButton));

                markup.setKeyboard(buttons);
                message.setReplyMarkup(markup);

                telegramBotService.getBot().execute(message); // Отправка сообщения через Telegram-бот

                return ResponseEntity.ok("Webhook processed successfully");
            }

            return ResponseEntity.ok("Payment not yet completed");
        } catch (Exception e) {
            logger.error("Ошибка при обработке webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }
}
