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
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    public void assignVpnToDevice(String chatId, String username, String plan) {
        try {
            VpnClient vpnClient = telegramBotService.assignClientToUser(chatId, username, "Phone");

            // Установка срока подписки
            LocalDateTime reservedUntil = switch (plan) {
                case "1 месяц" -> LocalDateTime.now().plusMonths(1);
                case "3 месяца" -> LocalDateTime.now().plusMonths(3);
                case "6 месяцев" -> LocalDateTime.now().plusMonths(6);
                case "1 год" -> LocalDateTime.now().plusYears(1);
                default -> throw new IllegalArgumentException("Invalid plan: " + plan);
            };
            vpnClient.setReservedUntil(reservedUntil);

            vpnClientRepository.save(vpnClient);

            // Уведомление пользователя
            sendSuccessMessage(chatId, plan);
        } catch (Exception e) {
            logger.error("Ошибка при выдаче VPN: ", e);
            sendErrorMessage(chatId, "Произошла ошибка. Попробуйте позже.");
        }
    }

    public void handleOsSelection(String chatId, String osType) {
        try {
            VpnClient client = vpnClientRepository.findFirstByChatId(chatId)
                    .orElseGet(() -> {
                        // Поиск доступного клиента
                        Optional<VpnClient> availableClient = vpnClientRepository
                                .findFirstByReservedUntilBeforeOrReservedUntilIsNull(LocalDateTime.now());
                        if (availableClient.isEmpty()) {
                            throw new RuntimeException("Нет доступных клиентов для назначения.");
                        }

                        VpnClient newClient = availableClient.get();
                        newClient.setChatId(chatId);
                        newClient.setReservedUntil(LocalDateTime.now().plusMinutes(15)); // Устанавливаем временную резервацию
                        return vpnClientRepository.save(newClient);
                    });

            // Устанавливаем ОС и сохраняем
            client.setOsType(osType);
            vpnClientRepository.save(client);

            logger.info("Пользователь {} выбрал ОС: {}", chatId, osType);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Вы выбрали операционную систему: " + osType + ". Продолжайте настройку.");
            telegramBotService.getBot().execute(message);

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

    private void sendSuccessMessage(String chatId, String plan) {
        try {
            String expirationMessage = switch (plan) {
                case "1 месяц" -> "Подписка активна до " + LocalDateTime.now().plusMonths(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                case "3 месяца" -> "Подписка активна до " + LocalDateTime.now().plusMonths(3).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                case "6 месяцев" -> "Подписка активна до " + LocalDateTime.now().plusMonths(6).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                case "1 год" -> "Подписка активна до " + LocalDateTime.now().plusYears(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                default -> "Не удалось определить срок подписки.";
            };

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Подписка на план \"" + plan + "\" успешно активирована!\n" + expirationMessage);

            telegramBotService.getBot().execute(message);
        } catch (Exception e) {
            logger.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
        }
    }
}
