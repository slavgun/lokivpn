package com.lokivpn.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
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

import java.io.File;


@Service
public class TelegramMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(TelegramMessageSender.class);
    private final AbsSender bot;

    public TelegramMessageSender(@Lazy AbsSender bot) {
        this.bot = bot;
    }

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


    public File getConfigFile(VpnClient vpnClient) {
        String remoteFilePath = vpnClient.getConfigFile();
        String localFilePath = "/etc/wireguard/configs/" + vpnClient.getClientName() + ".conf";
        try {
            // Используем SCP для загрузки файла
            Process process = Runtime.getRuntime().exec(new String[]{
                    "scp", "root@" + vpnClient.getServer() + ":" + remoteFilePath, localFilePath
            });
            process.waitFor();

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
        String remoteFilePath = vpnClient.getQrCodePath();
        String localFilePath = "/etc/wireguard/qrcodes/" + vpnClient.getClientName() + ".png";
        try {
            // Используем SCP для загрузки файла
            Process process = Runtime.getRuntime().exec(new String[]{
                    "scp", "root@" + vpnClient.getServer() + ":" + remoteFilePath, localFilePath
            });
            process.waitFor();

            // Проверяем, что файл был скачан
            File file = new File(localFilePath);
            if (file.exists()) {
                return file;
            } else {
                throw new RuntimeException("Не удалось скачать QR-код.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке QR-кода: " + e.getMessage(), e);
        }
    }


    public void sendFile(String chatId, String filePath, String caption) {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("Файл не найден: {}", filePath);
            sendMessage(chatId, "Ошибка: файл не найден.");
            return;
        }

        // Логика отправки файла в Telegram
        logger.info("Отправка файла {} пользователю {}", filePath, chatId);
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

