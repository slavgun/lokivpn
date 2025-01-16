package com.lokivpn.controller;

import com.lokivpn.service.DailyBillingService;
import com.lokivpn.service.TelegramBotService;
import com.lokivpn.service.TelegramMessageSender;
import com.lokivpn.config.SSHCommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final TelegramBotService telegramBotService;
    private final DailyBillingService dailyBillingService;
    private final TelegramMessageSender telegramMessageSender;

    public WebhookController(TelegramBotService telegramBotService,
                             DailyBillingService dailyBillingService,
                             TelegramMessageSender telegramMessageSender) {
        this.telegramBotService = telegramBotService;
        this.dailyBillingService = dailyBillingService;
        this.telegramMessageSender = telegramMessageSender;
    }

    @PostMapping
    public void onUpdateReceived(@RequestBody Update update) {
        try {
            logger.info("Received update: {}", update);

            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                String chatId = update.getMessage().getChatId().toString();

                if ("/process-billing".equals(messageText)) {
                    // Вызов биллинга
                    telegramMessageSender.sendMessage(chatId, "Запущен процесс биллинга...");
                    dailyBillingService.processDailyBalances();
                    telegramMessageSender.sendMessage(chatId, "Биллинг успешно завершен.");
                    return;
                }

                if ("/test-wg-genkey".equals(messageText)) {
                    // Выполнение команды wg genkey
                    telegramMessageSender.sendMessage(chatId, "Выполняется команда `wg genkey`...");
                    String result = SSHCommandExecutor.executeCommand(
                            "46.29.234.231",
                            "root",
                            "Ckfduey3103", // Замените на реальный пароль
                            "wg genkey"
                    );
                    telegramMessageSender.sendMessage(chatId, "Результат команды `wg genkey`: " + result);
                    return;
                }
            }

            // Обработка других сообщений
            telegramBotService.processUpdate(update);
        } catch (Exception e) {
            logger.error("Error processing update: {}", e.getMessage(), e);
        }
    }
}
