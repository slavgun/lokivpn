package com.lokivpn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class InstructionService {

    private static final Logger logger = LoggerFactory.getLogger(InstructionService.class);

    private final TelegramMessageSender telegramMessageService;

    public InstructionService(TelegramMessageSender telegramMessageService) {
        this.telegramMessageService = telegramMessageService;
    }

    public void sendDeviceInstructionMenu(String chatId) {
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

            InlineKeyboardButton androidTvButton = new InlineKeyboardButton();
            windowsButton.setText("📺 Android TV");
            windowsButton.setCallbackData("instruction_android_tv");

            // Создаем разметку кнопок
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> row1 = Collections.singletonList(iosButton);
            List<InlineKeyboardButton> row2 = Collections.singletonList(androidButton);
            List<InlineKeyboardButton> row3 = Collections.singletonList(windowsButton);
            List<InlineKeyboardButton> row4 = Collections.singletonList(androidTvButton);

            markup.setKeyboard(Arrays.asList(row1, row2, row3, row4));

            message.setReplyMarkup(markup);

            // Отправляем сообщение через TelegramMessageService
            telegramMessageService.sendMessage(message);
        } catch (Exception e) {
            logger.error("Ошибка отправки выбора устройства: {}", e.getMessage(), e);
        }
    }

    public void sendDeviceInstruction(String chatId, String deviceType) {
        try {
            String instructionText;

            switch (deviceType) {
                case "ios":
                    instructionText = """
                            Инструкция для <b>iOS</b>:
                            
                            1️⃣ Установите приложение <a href="https://apps.apple.com/app/wireguard/id1441195209">WireGuard</a> из App Store.
                            2️⃣ Скачайте конфигурацию и импортируйте её в приложение.
                            - Нажмите "<b>\uD83C\uDFE0 Личный кабинет</b>", далее "<b>\uD83D\uDD12 Мои VPN конфиги</b>", выберите нужный ключ и скачайте конфиг.
                            - Сохраните конфиг на свое устройство.
                            3️⃣ Зайдите в приложение <b>WireGuard</b>.
                            - Нажмите (<b>+</b>) в правом верхнем углу.
                            - "Создать из файла или архива".
                            - Найдите и нажмите на скачанный конфиг.
                            4️⃣ Нажмите "<b>Включить</b>" и наслаждайтесь подключением!
                            
                            Инструкция с картинками - <a href="https://telegra.ph/LOKI-VPN-dlya-ios-podklyuchenie-02-26">Посмотреть🔍</a>
                            """;
                    break;
                case "android":
                    instructionText = """
                            Инструкция для <b>Android</b>:
                            
                            1️⃣ Установите приложение <a href="https://play.google.com/store/apps/details?id=com.wireguard.android">WireGuard</a> из Google Play.
                            2️⃣ Скачайте конфигурацию и импортируйте её в приложение.
                            - Нажмите "<b>\uD83C\uDFE0 Личный кабинет</b>", далее "<b>\uD83D\uDD12 Мои VPN конфиги</b>", выберите нужный ключ и скачайте конфиг.
                            - Сохраните конфиг на свое устройство.
                            3️⃣ Зайдите в приложение <b>WireGuard</b>.
                            - Нажмите (<b>+</b>) в правом нижнем углу.
                            - "Import from file or archive".
                            - Найдите и нажмите на скачанный конфиг.
                            4️⃣ Нажмите "<b>Включить</b>" и наслаждайтесь подключением!
                            
                            Инструкция с картинками - <a href="https://telegra.ph/LOKI-VPN-dlya-android-podklyuchenie-02-26">Посмотреть🔍</a>
                            """;
                    break;
                case "windows":
                    instructionText = """
                            Инструкция для <b>Windows</b>:
                            
                            1️⃣ Установите приложение <a href="https://www.wireguard.com/install/">WireGuard</a> с официального сайта.
                            2️⃣ Скачайте конфигурацию и импортируйте её в приложение.
                            - Нажмите "<b>\uD83C\uDFE0 Личный кабинет</b>", далее "<b>\uD83D\uDD12 Мои VPN конфиги</b>", выберите нужный ключ и скачайте конфиг.
                            - Сохраните конфиг на свое устройство.
                            3️⃣ Импортируйте конфигурацию, нажав "<b>Добавить туннель</b>" слева внизу приложения.
                            4️⃣ Нажмите на конфиг и справа в окошке "<b>Включить</b>" для подключения.
                            
                            Инструкция с картинками - <a href="https://telegra.ph/LOKI-VPN-dlya-Windows-podklyuchenie-02-26">Посмотреть🔍</a>
                            """;
                    break;
                case "android_tv":
                    instructionText = """
                        Инструкция для <b>Android TV</b>:
                        
                        1️⃣ Установите приложение <a href="https://play.google.com/store/apps/details?id=com.wireguard.android">WireGuard</a> из Google Play Store на вашем Android TV.
                        2️⃣ Скачайте конфигурацию на другое устройство (например, на компьютер или телефон).
                        3️⃣ Передайте конфигурацию на Android TV с помощью USB-накопителя или отправьте файл через облачное хранилище (например, Google Drive).
                        - Если используете USB, подключите его к Android TV и откройте конфигурацию с помощью файлового менеджера.
                        - Если используете облачное хранилище, скачайте файл на Android TV.
                        4️⃣ Зайдите в приложение <b>WireGuard</b> на Android TV.
                        - Используя пульт, выберите (<b>+</b>) для добавления нового туннеля.
                        - Выберите опцию "Import from file or archive".
                        5️⃣ Найдите файл конфигурации и импортируйте его.
                        6️⃣ Выберите созданный туннель и нажмите "<b>Включить</b>" для подключения.
                        
                        Инструкция с картинками - <a href="https://telegra.ph/LOKI-VPN-dlya-android-TV-podklyuchenie-02-26">Посмотреть🔍</a>
                        """;
                    break;
                default:
                    instructionText = "Тип устройства не распознан.";
                    break;
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setParseMode("HTML");
            message.setText(instructionText);
            message.disableWebPagePreview();

            telegramMessageService.sendMessage(message);
        } catch (Exception e) {
            logger.error("Ошибка отправки инструкции: {}", e.getMessage(), e);
        }
    }
}