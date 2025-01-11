package com.lokivpn.bot.ui;

import com.lokivpn.bot.TelegramBot;
import com.lokivpn.service.AdminService;
import com.lokivpn.service.VpnProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);

    private final AdminService adminService;

    public MenuService(AdminService adminService) {
        this.adminService = adminService;
    }

    public void sendMainMenu(String chatId, TelegramLongPollingBot bot) {
        try {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);

            // Load image
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("images/loki.JPG");
            if (inputStream == null) {
                logger.error("Image not found in resources.");
                sendErrorMessage(chatId, bot, "⚠️ Ошибка загрузки изображения. Попробуйте позже.");
                return;
            }

            InputFile inputFile = new InputFile(inputStream, "loki.JPG");
            photo.setPhoto(inputFile);

            // Add caption
            photo.setCaption("""
            Добро пожаловать в LOKIVPN🐶

            Пока что сервис бесплатный, но если Вы хотите чтобы мой хозяин купил мне вкусняшки, можете перевести ему на карту. Напишите в поддержку, я скину реквизиты❣️
            """);

            // Create buttons
            InlineKeyboardButton purchaseButton = createButton("🔐 Получить VPN", "purchase_vpn", null);
            InlineKeyboardButton keysButton = createButton("🔑 Мои ключи", "my_keys", null);
            InlineKeyboardButton instructionButton = createButton("📖 Инструкция", "instruction", null);
            InlineKeyboardButton supportButton = createButton("🐶 Написать в поддержку", null, "https://t.me/lokivpn_support");

            // Add buttons to the list
            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            buttons.add(Collections.singletonList(purchaseButton));
            buttons.add(Collections.singletonList(keysButton));
            buttons.add(Collections.singletonList(instructionButton));
            buttons.add(Collections.singletonList(supportButton));

            // Check admin role and add the admin button if applicable
            if (adminService.isAdmin(chatId)) {
                InlineKeyboardButton adminButton = createButton("🛠️ Открыть админку", null, "https://t.me/lokivpnbot/lokivpn_admin");
                buttons.add(Collections.singletonList(adminButton));
            }

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            keyboardMarkup.setKeyboard(buttons);

            photo.setReplyMarkup(keyboardMarkup);

            bot.execute(photo);
        } catch (TelegramApiException e) {
            logger.error("Failed to send main menu: {}", e.getMessage(), e);
        }
    }

    public void handlePurchaseVpn(String chatId, TelegramLongPollingBot bot, VpnProvisionService vpnProvisionService) {
        vpnProvisionService.handleGetVpn(chatId, bot);
    }


    public void sendUnknownCommand(String chatId, TelegramLongPollingBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("❌ Команда не распознана. Попробуйте ещё раз или выберите действие из меню.");

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
        }
    }

    public void sendUnknownCallback(String chatId, TelegramLongPollingBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚠️ Обратный вызов не распознан. Попробуйте ещё раз.");

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке callback: {}", e.getMessage(), e);
        }
    }

    public void setupBotCommands(AbsSender bot) {
        List<BotCommand> commands = List.of(
                new BotCommand("/start", "\uD83C\uDFE1 Главное меню")
        );

        try {
            bot.execute(new SetMyCommands(commands, null, null));
        } catch (TelegramApiException e) {
            logger.error("Ошибка при настройке команд бота: {}", e.getMessage(), e);
        }
    }

    private InlineKeyboardButton createButton(String text, String callbackData, String url) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        if (callbackData != null) {
            button.setCallbackData(callbackData);
        }
        if (url != null) {
            button.setUrl(url);
        }
        return button;
    }

    private void sendErrorMessage(String chatId, TelegramLongPollingBot bot, String errorMessage) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(errorMessage);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
        }
    }
}