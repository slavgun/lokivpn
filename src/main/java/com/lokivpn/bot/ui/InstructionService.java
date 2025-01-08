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
import java.util.List;

@Service
public class InstructionService {

    private static final Logger logger = LoggerFactory.getLogger(InstructionService.class);

    public void sendDeviceInstructionMenu(String chatId, TelegramLongPollingBot bot) {
        try {
            // Создаем сообщение с текстом
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Выберите устройство, для которого требуется инструкция:");

            // Создаем кнопки для выбора устройств
            InlineKeyboardButton iosButton = new InlineKeyboardButton();
            iosButton.setText("📱 IOS");
            iosButton.setCallbackData("instruction_ios");

            InlineKeyboardButton androidButton = new InlineKeyboardButton();
            androidButton.setText("🤖 Android");
            androidButton.setCallbackData("instruction_android");

            InlineKeyboardButton windowsButton = new InlineKeyboardButton();
            windowsButton.setText("💻 Windows");
            windowsButton.setCallbackData("instruction_windows");

            // Создаем разметку кнопок
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> row1 = Collections.singletonList(iosButton);
            List<InlineKeyboardButton> row2 = Collections.singletonList(androidButton);
            List<InlineKeyboardButton> row3 = Collections.singletonList(windowsButton);

            markup.setKeyboard(Arrays.asList(row1, row2, row3));

            message.setReplyMarkup(markup);

            // Отправляем сообщение
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки выбора устройства: {}", e.getMessage(), e);
        }
    }

    public void sendDeviceInstruction(String chatId, String deviceType, TelegramLongPollingBot bot) {
        try {
            // Подготовка текста инструкции
            String instructionText;
            switch (deviceType) {
                case "ios":
                    instructionText = """
            Инструкция для <b>iOS</b>:
            
            1️⃣ Установите приложение <a href="https://apps.apple.com/app/wireguard/id1441195209">WireGuard</a> из App Store.
            2️⃣ Скачайте конфигурацию и импортируйте её в приложение.
            - Нажмите "<b>🔑 Мои ключи</b>", выберите нужный ключ и скачайте конфиг
            - Сохраните конфиг на свое устройство 
            3️⃣ Зайдите в приложение <b>WireGuard</b>
            - Нажмите (<b>+</b>) в правом верхнем углу
            - "Создать из файла или архива" 
            - Найдите и нажмите на скаченный конфиг
            4️⃣ Нажмите "<b>Включить</b>" и наслаждайтесь подключением!
            Тыкните на ползунок напротив названия конфига(ползунок загорится <b>зеленым</b> цветом)
            """;
                    break;
                case "android":
                    instructionText = """
            Инструкция для <b>Android</b>:
            
            1️⃣ Установите приложение <a href="https://play.google.com/store/apps/details?id=com.wireguard.android">WireGuard</a> из Google Play.
            2️⃣ Скачайте конфигурацию и импортируйте её в приложение.
            - Нажмите "<b>🔑 Мои ключи</b>", выберите нужный ключ и скачайте конфиг
            - Сохраните конфиг на свое устройство 
            3️⃣ Зайдите в приложение <b>WireGuard</b>
            - Нажмите (<b>+</b>) в правом нижнем углу
            - "Import from file or archive" 
            - Найдите и нажмите на скаченный конфиг
            4️⃣ Нажмите "<b>Включить</b>" и наслаждайтесь подключением!
            Тыкните на ползунок напротив названия конфига(ползунок загорится <b>голубым</b> цветом)
            """;
                    break;
                case "windows":
                    instructionText = """
            Инструкция для <b>Windows</b>:
            
            1️⃣ Установите приложение <a href="https://www.wireguard.com/install/">WireGuard</a> с официального сайта.
            2️⃣ Скачайте конфигурацию и импортируйте её в приложение.
            - Нажмите "<b>🔑 Мои ключи</b>", выберите нужный ключ и скачайте конфиг
            - Сохраните конфиг на свое устройство 
            3️⃣ Импортируйте конфигурацию, нажав "<b>Добавить туннель</b>" слева внизу приложения.
            4️⃣ Нажмите на конфиг и справа в окошке "<b>Включить</b>" для подключения.
            """;
                    break;
                default:
                    instructionText = "Тип устройства не распознан.";
                    break;
            }

            // Отправка инструкции
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setParseMode("HTML"); // Включаем HTML-разметку
            message.setText(instructionText);
            message.disableWebPagePreview(); // Отключаем предварительный просмотр ссылок

            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки инструкции: {}", e.getMessage(), e);
        }
    }
}
