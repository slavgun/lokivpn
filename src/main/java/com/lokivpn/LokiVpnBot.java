package com.lokivpn;

import com.lokivpn.service.TelegramBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class LokiVpnBot extends TelegramWebhookBot {

    private static final Logger logger = LoggerFactory.getLogger(LokiVpnBot.class);

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.bot-username}")
    private String botUsername;

    @Value("${telegram.webhook-url}")
    private String webhookPath;

    private final TelegramBotService telegramBotService;

    public LokiVpnBot(@Lazy TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotPath() {
        return webhookPath;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        logger.info("Received update: {}", update);
        try {
            telegramBotService.processUpdate(update);
        } catch (Exception e) {
            System.err.println("Error processing update: " + e.getMessage());
        }
        return null;
    }
}
