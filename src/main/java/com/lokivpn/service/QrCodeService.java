package com.lokivpn.service;

import com.lokivpn.bot.TelegramBotService;
import com.lokivpn.model.VpnClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.time.LocalDateTime;

@Service
public class QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeService.class);
    private final TelegramBotService telegramBotService;

    public QrCodeService(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    public void sendQrCode(String chatId, TelegramLongPollingBot bot) {
        try {
            // Получаем клиента по chatId
            VpnClient vpnClient = telegramBotService.getClientByChatId(chatId);
            if (vpnClient == null) {
                sendErrorMessage(bot, chatId, "QR-код не найден для вашего VPN клиента!");
                return;
            }

            // Проверка на блокировку
            if (vpnClient.getBlockedUntil() != null && vpnClient.getBlockedUntil().isAfter(LocalDateTime.now())) {
                sendErrorMessage(bot, chatId, "Ваш доступ временно заблокирован до " + vpnClient.getBlockedUntil());
                return;
            }

            // Получаем QR-код файла
            File qrCodeFile = telegramBotService.getQrCodeFile(vpnClient);
            if (qrCodeFile == null || !qrCodeFile.exists()) {
                sendErrorMessage(bot, chatId, "QR-код не найден!");
                return;
            }

            // Отправляем QR-код как документ
            SendDocument qrCode = new SendDocument();
            qrCode.setChatId(chatId);
            qrCode.setDocument(new InputFile(qrCodeFile));
            qrCode.setCaption("Ваш QR-код для настройки VPN:");

            bot.execute(qrCode);
        } catch (TelegramApiException e) {
            logger.error("Failed to send QR code: {}", e.getMessage(), e);
            sendErrorMessage(bot, chatId, "Не удалось отправить QR-код. Попробуйте позже.");
        } catch (RuntimeException e) {
            sendErrorMessage(bot, chatId, e.getMessage());
        }
    }

    private void sendErrorMessage(TelegramLongPollingBot bot, String chatId, String text) {
        try {
            SendDocument message = new SendDocument();
            message.setChatId(chatId);
            message.setCaption(text);
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения об ошибке: {}", e.getMessage(), e);
        }
    }
}
