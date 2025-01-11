package com.lokivpn.service;

import com.lokivpn.model.VpnClient;
import com.lokivpn.bot.TelegramBotService;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.repository.VpnClientRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;


@Service
public class VpnProvisionService {

    private static final Logger logger = LoggerFactory.getLogger(VpnProvisionService.class);

    private final TelegramBotService telegramBotService;

    @Autowired
    private VpnClientRepository vpnClientRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    public VpnProvisionService(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    public void handleGetVpn(String chatId, TelegramLongPollingBot bot) {
        logger.info("Начало обработки команды 'Получить VPN' для chatId={}", chatId);

        // Проверяем наличие незавершённой резервации
        Optional<VpnClient> optionalPendingClient = vpnClientRepository.findFirstByChatIdAndReservedUntilAfter(chatId, LocalDateTime.now());
        logger.debug("Результат проверки на наличие незавершённой резервации для chatId={}: {}", chatId, optionalPendingClient);

        if (optionalPendingClient.isPresent()) {
            VpnClient pendingClient = optionalPendingClient.get();
            logger.info("Найдена незавершённая резервация для chatId={}: {}", chatId, pendingClient);

            // Если есть незавершённая резервация, отправляем сообщение с кнопкой
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("У вас есть незавершённый платёж. Если хотите выбрать другой план подписки, нажмите на кнопку \"Выбрать другой план\".");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton resetPlanButton = new InlineKeyboardButton("Выбрать другой план");
            resetPlanButton.setCallbackData("reset_reservation");
            markup.setKeyboard(Collections.singletonList(Collections.singletonList(resetPlanButton)));
            message.setReplyMarkup(markup);

            try {
                bot.execute(message);
                logger.info("Сообщение о незавершённой резервации успешно отправлено для chatId={}", chatId);
            } catch (TelegramApiException e) {
                logger.error("Ошибка отправки сообщения о незавершённой резервации для chatId={}: {}", chatId, e.getMessage(), e);
            }
            return; // Завершаем выполнение метода
        }

        // Если резервации нет, запускаем процесс выбора устройства
        logger.info("Резервации для chatId={} не найдено. Запуск процесса выбора устройства.", chatId);
        sendDeviceSelection(chatId);
    }

    @Transactional
    public void resetReservation(String chatId) {
        // Находим последнюю активную резервацию пользователя
        Optional<VpnClient> lastReservation = vpnClientRepository
                .findFirstByChatIdAndReservedUntilAfter(chatId, LocalDateTime.now());

        if (lastReservation.isPresent()) {
            vpnClientRepository.delete(lastReservation.get()); // Удаляем только последнюю активную резервацию
            logger.info("Резервация для chatId={} успешно сброшена.", chatId);
        } else {
            logger.warn("Для chatId={} нет активной резервации для сброса.", chatId);
        }
    }

    public void sendDeviceSelection(String chatId) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Выберите для какого устройства сгенерировать конфигурацию VPN:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

            InlineKeyboardButton phoneButton = new InlineKeyboardButton("\uD83D\uDCF1 Смартфон");
            phoneButton.setCallbackData("device_phone");

            InlineKeyboardButton pcButton = new InlineKeyboardButton("\uD83D\uDCBB Компьютер");
            pcButton.setCallbackData("device_pc");

            markup.setKeyboard(Collections.singletonList(Arrays.asList(phoneButton, pcButton)));
            message.setReplyMarkup(markup);

            telegramBotService.getBot().execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки выбора устройства: {}", e.getMessage(), e);
        }
    }

    public void handleDeviceSelection(String chatId, String callbackData) {
        try {
            String deviceType;
            String osType = null; // Будет выбрано позже при выборе ОС

            // Обработка выбора устройства
            if (callbackData.equals("device_pc")) {
                deviceType = "ПК";
            } else if (callbackData.equals("device_phone")) {
                deviceType = "Смартфон";
            } else {
                throw new IllegalArgumentException("Некорректный выбор устройства");
            }

            // Обновление данных пользователя в базе
            updateDeviceForUser(chatId, deviceType, osType);

            // Отправка сообщения с выбором ОС
            sendOsSelectionMessage(chatId, deviceType);
        } catch (Exception e) {
            logger.error("Ошибка при обработке выбора устройства: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Ошибка при выборе устройства. Попробуйте снова.");
        }
    }

    public void updateDeviceForUser(String chatId, String deviceType, String osType) {
        try {
            VpnClient client = vpnClientRepository.findFirstByChatId(chatId)
                    .orElseThrow(() -> new RuntimeException("Клиент не найден для пользователя: " + chatId));

            client.setDeviceType(deviceType);
            client.setOsType(osType); // Обнуляем до выбора ОС
            vpnClientRepository.save(client);

            logger.info("Устройство для пользователя {} обновлено: {}, ОС обнулена.", chatId, deviceType);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении устройства для пользователя {}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendOsSelectionMessage(String chatId, String deviceType) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Вы выбрали устройство: " + deviceType + ". Пожалуйста, выберите операционную систему:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

            if ("ПК".equals(deviceType)) {
                // Для ПК
                InlineKeyboardButton windowsButton = new InlineKeyboardButton("🖥 Windows");
                windowsButton.setCallbackData("os_windows");
                InlineKeyboardButton macosButton = new InlineKeyboardButton("🍎 macOS");
                macosButton.setCallbackData("os_macos");
                InlineKeyboardButton linuxButton = new InlineKeyboardButton("🐧 Linux");
                linuxButton.setCallbackData("os_linux");

                buttons.add(Arrays.asList(windowsButton, macosButton));
                buttons.add(Collections.singletonList(linuxButton));
            } else {
                // Для смартфона
                InlineKeyboardButton iosButton = new InlineKeyboardButton("🍏 iOS");
                iosButton.setCallbackData("os_ios");
                InlineKeyboardButton androidButton = new InlineKeyboardButton("🤖 Android");
                androidButton.setCallbackData("os_android");

                buttons.add(Arrays.asList(iosButton, androidButton));
            }

            markup.setKeyboard(buttons);
            message.setReplyMarkup(markup);

            telegramBotService.getBot().execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения выбора ОС: {}", e.getMessage(), e);
        }
    }

    public void handleOsSelection(String chatId, String osType) {
        try {
            VpnClient client = vpnClientRepository.findFirstByChatId(chatId)
                    .orElseThrow(() -> new RuntimeException("Клиент не найден для пользователя: " + chatId));

            // Устанавливаем ОС и сохраняем
            client.setOsType(osType);
            vpnClientRepository.save(client);

            logger.info("Пользователь {} выбрал ОС: {}", chatId, osType);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Вы выбрали операционную систему: " + osType + ". Продолжайте настройку.");
            telegramBotService.getBot().execute(message);

            // Отправляем меню выбора плана подписки
            sendPlanSelectionMenu(chatId);
        } catch (RuntimeException e) {
            logger.error("Ошибка: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Клиент не найден или отсутствуют доступные клиенты. Пожалуйста, попробуйте снова.");
        } catch (TelegramApiException e) {
            logger.error("Ошибка обработки выбора ОС: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Произошла ошибка при обработке выбора ОС. Попробуйте снова.");
        }
    }

    public void sendPlanSelectionMenu(String chatId) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Выберите план подписки:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

            InlineKeyboardButton plan1Month = new InlineKeyboardButton("1 месяц - 200р.");
            plan1Month.setCallbackData("plan_1_month");

            InlineKeyboardButton plan3Months = new InlineKeyboardButton("3 месяца - 500р.");
            plan3Months.setCallbackData("plan_3_months");

            InlineKeyboardButton plan6Months = new InlineKeyboardButton("6 месяцев - 900р.");
            plan6Months.setCallbackData("plan_6_months");

            InlineKeyboardButton plan1Year = new InlineKeyboardButton("1 год - 1600р.");
            plan1Year.setCallbackData("plan_1_year");

            markup.setKeyboard(List.of(
                    List.of(plan1Month),
                    List.of(plan3Months),
                    List.of(plan6Months),
                    List.of(plan1Year)
            ));
            message.setReplyMarkup(markup);

            telegramBotService.getBot().execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке меню выбора плана: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Произошла ошибка при отображении меню выбора плана. Попробуйте позже.");
        }
    }

    private String normalizePlan(String callbackData) {
        switch (callbackData) {
            case "plan_1_month":
            case "1 месяц":
                return "1_month";
            case "plan_3_months":
            case "3 месяца":
                return "3_months";
            case "plan_6_months":
            case "6 месяцев":
                return "6_months";
            case "plan_1_year":
            case "1 год":
                return "1_year";
            default:
                throw new IllegalArgumentException("Unknown callbackData: " + callbackData);
        }
    }

    public void handlePlanSelection(String chatId, String callbackData, String username) {
        try {
            // Преобразуем callbackData в формат плана
            String plan = normalizePlan(callbackData);

            // Создаем или обновляем запись в vpn_clients
            VpnClient client = vpnClientRepository.findFirstByChatId(chatId)
                    .orElseGet(() -> {
                        VpnClient newClient = new VpnClient();
                        newClient.setChatId(chatId);
                        return vpnClientRepository.save(newClient);
                    });

            // Обновляем план и устанавливаем резервацию
            client.setPlan(plan);
            LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(15);
            client.setReservedUntil(reservedUntil);
            vpnClientRepository.save(client);

            logger.info("Резервация установлена для chatId={}, reservedUntil={}", chatId, reservedUntil);

            // Переход к оплате
            sendPaymentOptions(chatId, username, plan);
        } catch (Exception e) {
            logger.error("Ошибка обработки выбора плана: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Произошла ошибка при обработке выбора плана. Попробуйте снова.");
        }
    }


    public void sendPaymentOptions(String chatId, String username, String plan) {
        try {
            String paymentUrl = paymentService.createPaymentLink(Long.parseLong(chatId), username, plan);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Для продолжения, пожалуйста, оплатите подписку, нажав на кнопку ниже.");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

            InlineKeyboardButton paymentButton = new InlineKeyboardButton("\uD83D\uDCB3 Оплатить подписку");
            paymentButton.setUrl(paymentUrl);

            markup.setKeyboard(Collections.singletonList(Collections.singletonList(paymentButton)));
            message.setReplyMarkup(markup);

            telegramBotService.getBot().execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке кнопки оплаты: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Произошла ошибка при создании кнопки для оплаты. Попробуйте позже.");
        }
    }

    private void sendErrorMessage(String chatId, String text) {
        try {
            telegramBotService.getBot().execute(new SendMessage(chatId, text)); // Доступ к боту через TelegramBotService
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения об ошибке: {}", e.getMessage(), e);
        }
    }
}
