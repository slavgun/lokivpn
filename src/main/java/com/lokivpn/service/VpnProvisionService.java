package com.lokivpn.service;

import com.lokivpn.model.User;
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

        // Проверяем наличие незавершённой резервации или активной подписки
        Optional<VpnClient> optionalPendingClient = vpnClientRepository.findFirstByChatId(chatId);
        if (optionalPendingClient.isPresent()) {
            VpnClient pendingClient = optionalPendingClient.get();
            if (pendingClient.getReservedUntil() != null && pendingClient.getReservedUntil().isAfter(LocalDateTime.now())) {
                logger.info("Найдена незавершённая резервация для chatId={}: {}", chatId, pendingClient);

                sendMessageWithResetOption(chatId, bot, "У вас есть незавершённый платёж. Если хотите выбрать другой план подписки, нажмите на кнопку \"Выбрать другой план\".");
                return; // Завершаем выполнение метода
            } else if (pendingClient.getExpirationDate() != null && pendingClient.getExpirationDate().isAfter(LocalDateTime.now())) {
                logger.info("У пользователя {} уже есть активная подписка до {}", chatId, pendingClient.getExpirationDate());

                sendMessage(chatId, bot, "У вас уже есть активная подписка. Срок действия: " + pendingClient.getExpirationDate());
                return; // Завершаем выполнение метода
            }
        }

        // Если резервации и активной подписки нет, запускаем процесс выбора устройства
        logger.info("Резервации для chatId={} не найдено. Запуск процесса выбора устройства.", chatId);
        sendDeviceSelection(chatId);
    }

    private void sendMessageWithResetOption(String chatId, TelegramLongPollingBot bot, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton resetButton = new InlineKeyboardButton("Сбросить резервацию");
            resetButton.setCallbackData("reset_reservation");
            markup.setKeyboard(Collections.singletonList(Collections.singletonList(resetButton)));

            message.setReplyMarkup(markup);
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения с кнопкой сброса: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void resetReservation(String chatId) {
        Optional<VpnClient> client = vpnClientRepository.findFirstByChatId(chatId);
        if (client.isPresent()) {
            VpnClient vpnClient = client.get();
            vpnClient.setReservedUntil(null);
            vpnClient.setAssigned(false);
            vpnClient.setPlan(null);
            vpnClient.setExpirationDate(null);
            vpnClientRepository.save(vpnClient);
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
            String deviceType = callbackData.equals("device_pc") ? "ПК" : "Смартфон";

            // Ищем свободного клиента
            VpnClient client = vpnClientRepository.findFirstByIsAssignedFalse()
                    .orElseThrow(() -> new RuntimeException("Нет доступных клиентов для назначения."));

            // Привязываем нового клиента к пользователю
            client.setChatId(chatId);
            client.setDeviceType(deviceType);
            client.setAssigned(true);
            client.setReservedUntil(null); // Очищаем резервацию, если была
            vpnClientRepository.save(client);

            logger.info("Клиент {} назначен пользователю {} с устройством {}.", client.getId(), chatId, deviceType);

            // Отправка сообщения с выбором ОС
            sendOsSelectionMessage(chatId, deviceType);
        } catch (RuntimeException e) {
            logger.error("Ошибка назначения клиента: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Нет доступных клиентов. Попробуйте позже.");
        } catch (Exception e) {
            logger.error("Ошибка при выборе устройства: {}", e.getMessage(), e);
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
            // Ищем последнего назначенного клиента для пользователя
            VpnClient client = vpnClientRepository.findFirstByChatIdAndIsAssignedTrueOrderByIdDesc(chatId)
                    .orElseThrow(() -> new RuntimeException("Клиент не найден для пользователя: " + chatId));

            // Устанавливаем ОС
            client.setOsType(osType);
            vpnClientRepository.save(client);

            logger.info("Пользователь {} выбрал ОС {} для клиента {}.", chatId, osType, client.getId());

            sendPlanSelectionMenu(chatId);
        } catch (RuntimeException e) {
            logger.error("Ошибка: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Ошибка при выборе ОС. Попробуйте позже.");
        } catch (Exception e) {
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

    public void handlePlanSelection(String chatId, String callbackData, String username) {
        try {
            // Нормализуем план
            String normalizedPlan = normalizePlan(callbackData);

            // Ищем последнего назначенного клиента для пользователя
            VpnClient client = vpnClientRepository.findFirstByChatIdAndIsAssignedTrueOrderByIdDesc(chatId)
                    .orElseThrow(() -> new RuntimeException("Нет клиента для назначения плана."));

            // Обновляем информацию о плане
            client.setPlan(normalizedPlan);
            client.setReservedUntil(LocalDateTime.now().plusMinutes(15));
            vpnClientRepository.save(client);

            logger.info("Резервация плана {} установлена для клиента {} (пользователь {}).", normalizedPlan, client.getId(), chatId);

            // Переход к оплате
            sendPaymentOptions(chatId, username, normalizedPlan);
        } catch (Exception e) {
            logger.error("Ошибка обработки выбора плана: {}", e.getMessage(), e);
            sendErrorMessage(chatId, "Произошла ошибка при обработке выбора плана. Попробуйте снова.");
        }
    }


    private String normalizePlan(String callbackData) {
        switch (callbackData) {
            case "plan_1_month":
                return "1_month";
            case "plan_3_months":
                return "3_months";
            case "plan_6_months":
                return "6_months";
            case "plan_1_year":
                return "1_year";
            default:
                throw new IllegalArgumentException("Unknown plan: " + callbackData);
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

    private void sendMessage(String chatId, TelegramLongPollingBot bot, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения: {}", e.getMessage(), e);
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
