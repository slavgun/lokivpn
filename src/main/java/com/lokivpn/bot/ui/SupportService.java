package com.lokivpn.bot.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.Collections;

@Service
public class SupportService {
    private static final Logger logger = LoggerFactory.getLogger(SupportService.class);

    /**
     * Отправляет сообщение с информацией о поддержке и кнопками.
     */
    public void sendSupportInfo(String chatId, TelegramLongPollingBot bot) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🐶 Свяжитесь с нашей поддержкой"); // Текст сообщения

        // Создаем кнопки
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        // Кнопка "Написать в поддержку"
        InlineKeyboardButton supportButton = new InlineKeyboardButton("Написать в поддержку");
        supportButton.setUrl("https://t.me/lokivpn_support"); // Ссылка на поддержку

        // Кнопка "⬅️ Назад в меню"
        InlineKeyboardButton backButton = new InlineKeyboardButton("⬅️ Назад в меню");
        backButton.setCallbackData("main_menu"); // Callback для возврата в главное меню

        // Устанавливаем кнопки в клавиатуру
        keyboardMarkup.setKeyboard(Arrays.asList(
                Collections.singletonList(supportButton), // Кнопка "Написать в поддержку"
                Collections.singletonList(backButton)    // Кнопка "⬅️ Назад в меню"
        ));

        message.setReplyMarkup(keyboardMarkup);

        try {
            bot.execute(message); // Отправляем сообщение
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения с информацией о поддержке: {}", e.getMessage(), e);
        }
    }
}

