package com.lokivpn.service;

import com.lokivpn.model.VpnClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


@Service
public class TelegramMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(TelegramMessageSender.class);
    private final AbsSender bot;

    public TelegramMessageSender(@Lazy AbsSender bot) {
        this.bot = bot;
    }

// Кастомка

    public void sendCustomNotification(Long chatId, String message, String photoUrl, List<String> buttonTexts, List<String> buttonUrls) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            sendPhotoNotification(chatId, message, photoUrl, buttonTexts, buttonUrls);
        } else {
            sendTextNotification(chatId, message, buttonTexts, buttonUrls);
        }
    }

    private void sendTextNotification(Long chatId, String message, List<String> buttonTexts, List<String> buttonUrls) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(message);

        if (buttonTexts != null && buttonUrls != null && !buttonTexts.isEmpty() && !buttonUrls.isEmpty()) {
            InlineKeyboardMarkup markup = createInlineKeyboard(buttonTexts, buttonUrls);
            sendMessage.setReplyMarkup(markup);
        }

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Failed to send text message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    private void sendPhotoNotification(Long chatId, String message, String photoUrl, List<String> buttonTexts, List<String> buttonUrls) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(new InputFile(photoUrl));
        sendPhoto.setCaption(message);

        if (buttonTexts != null && buttonUrls != null && !buttonTexts.isEmpty() && !buttonUrls.isEmpty()) {
            InlineKeyboardMarkup markup = createInlineKeyboard(buttonTexts, buttonUrls);
            sendPhoto.setReplyMarkup(markup);
        }

        try {
            bot.execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.error("Failed to send photo message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    private InlineKeyboardMarkup createInlineKeyboard(List<String> buttonTexts, List<String> buttonUrls) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < buttonTexts.size() && i < buttonUrls.size(); i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonTexts.get(i));
            button.setUrl(buttonUrls.get(i));

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            rows.add(row);
        }

        markup.setKeyboard(rows);
        return markup;
    }

// Методы для отправки сообщений

    public void sendMessage(String chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
        }
    }

    public void sendMessage(SendMessage sendMessage) {
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
        }
    }

    public void sendMessage(String chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(markup);

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    // Новый метод для добавления parseMode
    public void sendMessage(String chatId, String text, InlineKeyboardMarkup markup, String parseMode) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(markup);
        sendMessage.setParseMode(parseMode); // Установка режима разметки

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения: {}", e.getMessage(), e);
        }
    }

    // Отправка сообщения с фото
    public void sendPhoto(SendPhoto sendPhoto) {
        try {
            bot.execute(sendPhoto); // Отправляет фото через Telegram API
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке фото: {}", e.getMessage(), e);
        }
    }


    // Отправка уведомления
    public void sendNotification(Long userId, String message) {
        try {
            String chatId = String.valueOf(userId);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);

            bot.execute(sendMessage);
            logger.info("Notification sent to user {}: {}", userId, message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

// Методы отправки конфигураций

    public File getConfigFile(VpnClient vpnClient) {
        String remoteFilePath = vpnClient.getConfigFile();
        String localFilePath = "/tmp/" + vpnClient.getClientName() + ".conf"; // Временный путь

        logger.info("Начинаем скачивание файла {} с сервера {}", remoteFilePath, vpnClient.getServer());

        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "scp", "root@" + vpnClient.getServer() + ":" + remoteFilePath, localFilePath
            });

            // Чтение потока ошибок SCP
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            errorReader.close();

            // Ожидание завершения процесса
            process.waitFor();
            logger.info("Команда SCP завершилась с кодом: {}", process.exitValue());

            if (process.exitValue() != 0) {
                logger.error("Ошибка SCP: {}", errorOutput.toString());
                throw new RuntimeException("Ошибка SCP: " + errorOutput.toString());
            }

            // Проверяем, что файл скачался
            File file = new File(localFilePath);
            if (file.exists()) {
                logger.info("Файл успешно скачан: {}", file.getAbsolutePath());
                return file;
            } else {
                logger.error("Файл не найден после SCP: {}", localFilePath);
                throw new RuntimeException("Файл не найден после SCP: " + localFilePath);
            }
        } catch (Exception e) {
            logger.error("Ошибка при загрузке файла конфигурации: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при загрузке файла конфигурации: " + e.getMessage(), e);
        }
    }


    public void sendFile(String chatId, File file, String caption) {
        if (!file.exists()) {
            logger.error("Файл не найден: {}", file.getAbsolutePath());
            sendMessage(chatId, "Ошибка: файл не найден.");
            return;
        }

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(file));
        sendDocument.setCaption(caption);

        try {
            bot.execute(sendDocument);
            logger.info("Файл {} успешно отправлен пользователю {}", file.getAbsolutePath(), chatId);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки файла: {}", e.getMessage(), e);
            sendMessage(chatId, "Ошибка при отправке файла.");
        }
    }

    public void sendFile(String chatId, String filePath, String caption) {
        sendFile(chatId, new File(filePath), caption);
    }

    public void sendInvoice(SendInvoice invoice) {
        try {
            bot.execute(invoice);
        } catch (TelegramApiException e) {
            logger.error("Error sending invoice: {}", e.getMessage(), e);
        }
    }

    public void sendPreCheckoutQuery(AnswerPreCheckoutQuery answer) {
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            logger.error("Error sending pre-checkout query: {}", e.getMessage(), e);
        }
    }

    // Метод отправки APK

    public void sendApkFile(String chatId, String filePath) {
        File apkFile = new File(filePath);

        if (!apkFile.exists()) {
            logger.error("Файл APK не найден: {}", apkFile.getAbsolutePath());
            sendMessage(chatId, "❌ Ошибка: APK-файл не найден.");
            return;
        }

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(apkFile));
        sendDocument.setCaption("📲 Установите приложение Loki VPN и следуйте инструкции ниже.");

        try {
            bot.execute(sendDocument);
            logger.info("APK-файл {} успешно отправлен пользователю {}", apkFile.getAbsolutePath(), chatId);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки APK-файла: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при отправке APK-файла.");
        }
    }
}

