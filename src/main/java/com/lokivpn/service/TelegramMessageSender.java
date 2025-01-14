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
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;


@Service
public class TelegramMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(TelegramMessageSender.class);
    private final AbsSender bot;

    public TelegramMessageSender(@Lazy AbsSender bot) {
        this.bot = bot;
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
        String localFilePath = "/tmp/" + vpnClient.getClientName() + ".conf"; // Используем временный путь
        try {
            // Используем SCP для загрузки файла
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

            // Проверка завершения команды
            if (process.exitValue() != 0) {
                throw new RuntimeException("Ошибка SCP: " + errorOutput.toString());
            }

            // Проверяем, что файл был скачан
            File file = new File(localFilePath);
            if (file.exists()) {
                return file;
            } else {
                throw new RuntimeException("Не удалось скачать файл конфигурации.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файла конфигурации: " + e.getMessage(), e);
        }
    }

    public File getQrCodeFile(VpnClient vpnClient) {
        // Получаем путь удаленного файла и временный путь для сохранения локально
        String remoteFilePath = vpnClient.getQrCodePath();
        String localFilePath = "/tmp/" + vpnClient.getClientName() + ".png"; // Используем временный путь

        try {
            // Формируем и выполняем SCP команду
            Process process = Runtime.getRuntime().exec(new String[]{
                    "scp", "root@" + vpnClient.getServer() + ":" + remoteFilePath, localFilePath
            });

            // Читаем поток ошибок для логирования
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            errorReader.close();

            // Ожидаем завершения процесса
            process.waitFor();

            // Проверяем завершение процесса на успешность
            if (process.exitValue() != 0) {
                throw new RuntimeException("Ошибка SCP: " + errorOutput.toString());
            }

            // Проверяем наличие загруженного файла
            File file = new File(localFilePath);
            if (file.exists()) {
                logger.info("QR-код успешно загружен: {}", localFilePath);
                return file;
            } else {
                throw new RuntimeException("Не удалось скачать QR-код: файл не найден локально.");
            }
        } catch (Exception e) {
            // Логируем ошибку и выбрасываем исключение
            logger.error("Ошибка при загрузке QR-кода: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при загрузке QR-кода: " + e.getMessage(), e);
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
}

