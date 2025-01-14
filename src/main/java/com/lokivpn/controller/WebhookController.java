package com.lokivpn.controller;

import com.lokivpn.service.TelegramBotService;
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

    public WebhookController(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @PostMapping
    public void onUpdateReceived(@RequestBody Update update) {
        try {
            logger.info("Received update: {}", update);
            telegramBotService.processUpdate(update);
        } catch (Exception e) {
            logger.error("Error processing update: {}", e.getMessage(), e);
        }
    }
}
