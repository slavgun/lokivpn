package com.lokivpn.bot;

import com.lokivpn.bot.ui.MenuService;
import com.lokivpn.bot.ui.InstructionService;
import com.lokivpn.bot.ui.SupportService;
import com.lokivpn.service.KeyService;
import com.lokivpn.service.ConfigFileService;
import com.lokivpn.service.QrCodeService;
import com.lokivpn.service.VpnProvisionService;
import com.lokivpn.model.User;
import com.lokivpn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final TelegramBotService telegramBotService;
    private final MenuService menuService;
    private final InstructionService instructionService;
    private final KeyService keyService;
    private final ConfigFileService configFileService;
    private final QrCodeService qrCodeService;
    private final VpnProvisionService vpnProvisionService; // Новый сервис
    private final SupportService supportService;
    private final UserRepository userRepository;

    public TelegramBot(
            TelegramBotService telegramBotService,
            MenuService menuService,
            InstructionService instructionService,
            KeyService keyService,
            ConfigFileService configFileService,
            QrCodeService qrCodeService,
            SupportService supportService, // Этот параметр добавлен
            VpnProvisionService vpnProvisionService,
            UserRepository userRepository
    ) {
        this.telegramBotService = telegramBotService;
        this.menuService = menuService;
        this.instructionService = instructionService;
        this.keyService = keyService;
        this.configFileService = configFileService;
        this.qrCodeService = qrCodeService;
        this.supportService = supportService; // Убедитесь, что это поле тоже объявлено
        this.vpnProvisionService = vpnProvisionService;
        this.userRepository = userRepository;

        setCommandsForMenu();
    }

    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (RuntimeException e) {
            logger.error("Ошибка выполнения: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Неизвестная ошибка: {}", e.getMessage(), e);
        }
    }

    // Обработка текстовых сообщений
    private void handleTextMessage(Update update) {
        String messageText = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        String username = update.getMessage().getFrom().getUserName();
        Integer messageId = update.getMessage().getMessageId();

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole("USER");
                    return newUser;
                });

        user.setChatId(chatId);
        userRepository.save(user);

        switch (messageText) {
            case "/start":
                logger.info("Обработка команды /start от пользователя: {}", chatId);
                menuService.sendMainMenu(chatId, this);
                break;
            default:
                logger.warn("Неизвестная команда: {}", messageText);
                menuService.sendUnknownCommand(chatId, this);
                sendMessage(chatId, "Команда не распознана. Попробуйте ещё раз.");
                deleteMessage(chatId, messageId);
                break;
        }
    }

    // Обработка callback-запросов
    private void handleCallbackQuery(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String callbackData = update.getCallbackQuery().getData();
        String username = update.getCallbackQuery().getFrom().getUserName();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        logger.info("Получены callback-данные: {}", callbackData);

        switch (callbackData) {
            case "support":
                supportService.sendSupportInfo(chatId, this);
                break;
            case "main_menu":
                menuService.sendMainMenu(chatId, this);
                break;
            case "show_qr":
                qrCodeService.sendQrCode(chatId, this);
                break;
            case "download_config":
                configFileService.sendConfigFile(chatId, this);
                break;
            case "my_keys":
                keyService.sendKeysMenu(chatId, this);
                break;
            case "purchase_vpn":
                menuService.handlePurchaseVpn(chatId, this, vpnProvisionService);
                break;
            case "instruction":
                instructionService.sendDeviceInstructionMenu(chatId, this);
                break;
            case "reset_reservation":
                vpnProvisionService.resetReservation(chatId);
                vpnProvisionService.sendDeviceSelection(chatId);
                break;
            case "device_phone":
            case "device_pc":
                vpnProvisionService.handleDeviceSelection(chatId, callbackData);
                break;
            case "os_android":
            case "os_ios":
            case "os_windows":
            case "os_macos":
                vpnProvisionService.handleOsSelection(chatId, callbackData.replace("instruction_", ""));;
                break;
            case "instruction_ios":
            case "instruction_android":
            case "instruction_windows":
                // Убираем префикс "instruction_" перед передачей в метод
                String deviceType = callbackData.replace("instruction_", "");
                instructionService.sendDeviceInstruction(chatId, deviceType, this);
                break;
            default:
                if (callbackData.startsWith("plan_")) {
                    processPlanSelection(chatId, callbackData, username, messageId);
                } else {
                    logger.warn("Неизвестные callback-данные: {}", callbackData);
                    menuService.sendUnknownCallback(chatId, this);
                }
                break;
        }
    }

    // Обработка выбора плана
    private void processPlanSelection(String chatId, String callbackData, String username, Integer messageId) {
        if (isRequestTooFrequent(chatId)) {
            sendMessage(chatId, "Пожалуйста, подождите несколько секунд перед повторным нажатием.");
            return;
        }

        disableButtons(chatId, messageId);

        String plan = PLAN_MAPPING.get(callbackData);
        if (plan == null) {
            logger.warn("Неизвестный план подписки: {}", callbackData);
            menuService.sendUnknownCallback(chatId, this);
            return;
        }

        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getLastPlanSelected() != null &&
                user.getLastPlanSelected().equals(plan) &&
                user.getLastPlanTime() != null &&
                Duration.between(user.getLastPlanTime(), Instant.now()).toMinutes() < 5) {
            sendMessage(chatId, "Вы уже выбрали этот план. Пожалуйста, завершите оплату.");
            return;
        }

        user.setLastPlanSelected(plan);
        user.setLastPlanTime(Instant.now());
        userRepository.save(user);

        vpnProvisionService.handlePlanSelection(chatId, callbackData, username);
    }

    // Карта планов
    private static final Map<String, String> PLAN_MAPPING = Map.of(
            "plan_1_month", "1 месяц",
            "plan_3_months", "3 месяца",
            "plan_6_months", "6 месяцев",
            "plan_1_year", "1 год"
    );

    // Ограничение частоты запросов
    private final Map<String, Instant> requestTimestamps = new HashMap<>();

    private boolean isRequestTooFrequent(String chatId) {
        Instant lastRequestTime = requestTimestamps.get(chatId);
        if (lastRequestTime == null || Duration.between(lastRequestTime, Instant.now()).getSeconds() > 3) {
            requestTimestamps.put(chatId, Instant.now());
            return false;
        }
        return true;
    }

    // Отключение кнопок
    private void disableButtons(String chatId, Integer messageId) {
        try {
            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            editMarkup.setChatId(chatId);
            editMarkup.setMessageId(messageId);
            editMarkup.setReplyMarkup(null);
            execute(editMarkup);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отключении кнопок: {}", e.getMessage(), e);
        }
    }

    // Утилита для отправки сообщения
    private void sendMessage(String chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения: {}", e.getMessage(), e);
        }
    }

    // Удаление сообщения
    private void deleteMessage(String chatId, Integer messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            if (!e.getMessage().contains("message to delete not found")) {
                logger.error("Ошибка при удалении сообщения: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public String getBotToken() {
        return "7998813087:AAGKaewz7JDgogk3WDT9O0gKzf52NYKBC-k";
    }

    @Override
    public String getBotUsername() {
        return "lokivpnbot";
    }

    private void setCommandsForMenu() {
        menuService.setupBotCommands(this);
    }
}
