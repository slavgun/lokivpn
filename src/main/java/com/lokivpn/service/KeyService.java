package com.lokivpn.service;

import com.lokivpn.bot.TelegramBotService;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.VpnClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class KeyService {

    private static final Logger logger = LoggerFactory.getLogger(KeyService.class);
    private final TelegramBotService telegramBotService;

    @Autowired
    private VpnClientRepository vpnClientRepository;

    public KeyService(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    public void sendKeysMenu(String chatId, TelegramLongPollingBot bot) {
        try {
            // Получаем список активных и зарезервированных клиентов
            List<VpnClient> clients = vpnClientRepository.findActiveAndReservedClientsByChatId(chatId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);

            if (clients.isEmpty()) {
                message.setText("У вас нет активных ключей.");
            } else {
                message.setText("Ваши ключи:");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

                for (VpnClient client : clients) {
                    String deviceType = client.getDeviceType() != null ? client.getDeviceType() : "Устройство";

                    logger.info("Device type: {}, Client name: {}, Is assigned: {}, Reserved until: {}",
                            deviceType, client.getClientName(), client.isAssigned(), client.getReservedUntil());

                    String emoji;
                    if (client.isAssigned()) {
                        // Ключ активен
                        emoji = client.getDeviceType() != null && client.getDeviceType().equalsIgnoreCase("pc") ? "🖥️" : "📱";
                    } else if (client.getReservedUntil() != null && client.getReservedUntil().isAfter(LocalDateTime.now())) {
                        // Ключ зарезервирован
                        emoji = "🔑";
                    } else {
                        // Неопределенный статус
                        emoji = "❌";
                    }

                    InlineKeyboardButton button = new InlineKeyboardButton(emoji + " " + deviceType + " (" + client.getClientName() + ")");
                    button.setCallbackData(client.isAssigned() ? "key_" + client.getId() : "reserved_" + client.getId());
                    buttons.add(Collections.singletonList(button));
                }

                markup.setKeyboard(buttons);
                message.setReplyMarkup(markup);
            }

            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки списка ключей: {}", e.getMessage(), e);
        }
    }

    public void handleDeviceCallback(String chatId, String callbackData, TelegramLongPollingBot bot) {
        try {
            String[] parts = callbackData.split("_");
            String action = parts[0];
            String clientId = parts[1];

            VpnClient client = telegramBotService.getClientById(clientId);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);

            if ("key".equals(action)) {
                // Ключ активен — позволяем скачать конфигурацию
                message.setText("Ваш ключ для устройства " + client.getDeviceType() + ":");
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

                InlineKeyboardButton qrButton = new InlineKeyboardButton("📷 Показать QR код");
                qrButton.setCallbackData("show_qr_" + client.getId());

                InlineKeyboardButton configButton = new InlineKeyboardButton("📂 Скачать конфиг");
                configButton.setCallbackData("download_config_" + client.getId());

                markup.setKeyboard(Arrays.asList(
                        Arrays.asList(qrButton, configButton)
                ));
                message.setReplyMarkup(markup);
            } else if ("reserved".equals(action)) {
                // Ключ зарезервирован — сообщаем о необходимости оплаты
                message.setText("Этот ключ зарезервирован. Пожалуйста, оплатите его, чтобы получить доступ.");
            }

            bot.execute(message);
        } catch (Exception e) {
            logger.error("Ошибка обработки callback для устройства: {}", e.getMessage(), e);
        }
    }
}
