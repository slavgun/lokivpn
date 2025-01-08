package com.lokivpn.service;

import com.lokivpn.bot.TelegramBotService;
import com.lokivpn.model.VpnClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.LocalDateTime;

@Service
public class ConfigFileService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigFileService.class);
    private final TelegramBotService telegramBotService;

    public ConfigFileService(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }


    public void sendConfigFile(String chatId, TelegramLongPollingBot bot) {
        try {
            VpnClient vpnClient = telegramBotService.getClientByChatId(chatId);

            if (vpnClient == null) {
                sendErrorMessage(chatId, bot, "Файл конфигурации не найден!");
                return;
            }

            // Проверка на блокировку
            if (vpnClient.getBlockedUntil() != null && vpnClient.getBlockedUntil().isAfter(LocalDateTime.now())) {
                sendErrorMessage(chatId, bot, "Ваш доступ временно заблокирован до " + vpnClient.getBlockedUntil());
                return;
            }

            File configFile = telegramBotService.getConfigFile(vpnClient);
            if (configFile == null || !configFile.exists()) {
                sendErrorMessage(chatId, bot, "Файл конфигурации не найден!");
                return;
            }

            SendDocument document = new SendDocument();
            document.setChatId(chatId);
            document.setDocument(new InputFile(configFile));
            document.setCaption("Ваш файл конфигурации WireGuard VPN:");

            bot.execute(document);
        } catch (TelegramApiException e) {
            logger.error("Failed to send config file: {}", e.getMessage(), e);
            sendErrorMessage(chatId, bot, "Не удалось отправить файл конфигурации.");
        }
    }

    private void sendErrorMessage(String chatId, TelegramLongPollingBot bot, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send error message: {}", e.getMessage(), e);
        }
    }
}

