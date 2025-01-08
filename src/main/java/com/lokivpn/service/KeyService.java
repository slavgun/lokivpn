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

                    String emoji;
                    switch (deviceType.toLowerCase()) {
                        case "pc":
                            emoji = "🖥️";
                            break;
                        case "phone":
                            emoji = "📱";
                            break;
                        default:
                            emoji = "🔑";
                            break;
                    }

                    InlineKeyboardButton button = new InlineKeyboardButton(emoji + " " + deviceType + " (" + client.getClientName() + ")");
                    button.setCallbackData("key_" + client.getId());
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

    public void deleteConfiguration(String chatId, String clientId, TelegramLongPollingBot bot) {
        try {
            // Находим конфигурацию по clientId
            VpnClient client = telegramBotService.getClientById(clientId);

            // Убираем связь конфигурации с пользователем
            client.setAssigned(false);
            client.setChatId(null);
            client.setUsername(null);
            client.setDeviceType(null); // Сбрасываем тип устройства
            telegramBotService.saveClient(client);

            // Уведомляем пользователя об успешном удалении
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("✅ Конфигурация для устройства " + client.getDeviceType() + " успешно удалена.");
            bot.execute(message);
        } catch (Exception e) {
            logger.error("Ошибка при удалении конфигурации: {}", e.getMessage(), e);

            // Отправляем сообщение об ошибке пользователю
            sendErrorMessage(chatId, "❌ Ошибка при удалении конфигурации. Попробуйте позже.", bot);
        }
    }

    public void handleDeviceCallback(String chatId, String callbackData, TelegramLongPollingBot bot) {
        try {
            // Извлекаем clientId из callbackData
            String clientId = callbackData.split("_")[1];
            VpnClient client = telegramBotService.getClientById(clientId);

            // Формируем сообщение с информацией о клиенте
            SendMessage message = new SendMessage();
            message.setChatId(chatId);

            // Формируем текст с использованием MarkdownV2 для скрытого ключа
            String hiddenKey = "Ваш ключ для устройства " + client.getDeviceType() + ":\n||" + escapeMarkdownV2(client.getClientPublicKey()) + "||";
            message.setText(hiddenKey);

            // Указываем использование MarkdownV2
            message.setParseMode("MarkdownV2");

            // Добавляем кнопки для QR-кода, файла конфигурации и удаления
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton qrButton = new InlineKeyboardButton("📷 Показать QR код");
            qrButton.setCallbackData("show_qr_" + client.getId());

            InlineKeyboardButton configButton = new InlineKeyboardButton("📂 Скачать конфиг");
            configButton.setCallbackData("download_config_" + client.getId());

            InlineKeyboardButton deleteButton = new InlineKeyboardButton("❌ Удалить конфигурацию");
            deleteButton.setCallbackData("delete_config_" + client.getId());

            // Размещаем кнопки: первые две на одной строке, третью на отдельной строке
            markup.setKeyboard(Arrays.asList(
                    Arrays.asList(qrButton, configButton), // Первая строка: QR-код и Скачать конфиг
                    Collections.singletonList(deleteButton) // Вторая строка: Удалить конфигурацию
            ));

            message.setReplyMarkup(markup);

            bot.execute(message);
        } catch (Exception e) {
            logger.error("Ошибка обработки callback для устройства: {}", e.getMessage(), e);
        }
    }

    private String escapeMarkdownV2(String text) {
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    private void sendErrorMessage(String chatId, String text, TelegramLongPollingBot bot) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения об ошибке: {}", e.getMessage(), e);
        }
    }
}
