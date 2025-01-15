package com.lokivpn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

@Service
public class SupportService {

    private static final Logger logger = LoggerFactory.getLogger(SupportService.class);

    private final TelegramMessageSender telegramMessageService;

    public SupportService(TelegramMessageSender telegramMessageService) {
        this.telegramMessageService = telegramMessageService;
    }

    public void sendSupportInfo(String chatId) {
        // Создаем кнопки
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        // Кнопка "Написать в поддержку"
        InlineKeyboardButton supportButton = new InlineKeyboardButton("✏\uFE0F Написать в поддержку");
        supportButton.setUrl("https://t.me/lokivpn_support"); // Ссылка на поддержку

        // Кнопка "⬅️ Назад в меню"
        InlineKeyboardButton backButton = new InlineKeyboardButton("⬅️ Назад в меню");
        backButton.setCallbackData("main_menu"); // Callback для возврата в главное меню

        // Устанавливаем кнопки в клавиатуру
        keyboardMarkup.setKeyboard(Arrays.asList(
                Collections.singletonList(supportButton), // Кнопка "Написать в поддержку"
                Collections.singletonList(backButton)    // Кнопка "⬅️ Назад в меню"
        ));

        // Путь к фото в ресурсах
        String photoPath = "images/loki_cab.jpg"; // Путь внутри папки resources

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(photoPath)) {
            if (inputStream == null) {
                throw new NullPointerException("Фото не найдено в ресурсах: " + photoPath);
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(inputStream, "loki_cab.jpg"));
            sendPhoto.setCaption("\uD83D\uDC36 Свяжитесь с нашей поддержкой!");
            sendPhoto.setReplyMarkup(keyboardMarkup);

            telegramMessageService.sendPhoto(sendPhoto);
        } catch (Exception e) {
            // Логирование и fallback
            logger.error("Не удалось загрузить изображение: {}", e.getMessage());
            SendMessage fallbackMessage = new SendMessage();
            fallbackMessage.setChatId(chatId);
            fallbackMessage.setText("\uD83D\uDC36 Свяжитесь с нашей поддержкой!\n\n" +
                    "Напишите в поддержку, используя кнопку ниже.");
            fallbackMessage.setReplyMarkup(keyboardMarkup);
            telegramMessageService.sendMessage(fallbackMessage);
        }
    }
}


