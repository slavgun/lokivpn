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
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
            // Обработка текстовых сообщений
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                String chatId = update.getMessage().getChatId().toString();
                String username = update.getMessage().getFrom().getUserName();
                Integer messageId = update.getMessage().getMessageId(); // ID сообщения для удаления

                User user = userRepository.findByUsername(username)
                        .orElseGet(() -> {
                            User newUser = new User();
                            newUser.setUsername(username);
                            newUser.setRole("USER"); // Устанавливаем роль по умолчанию
                            return newUser;
                        });

                // Обновляем данные пользователя
                user.setChatId(chatId);
                userRepository.save(user);

                switch (messageText) {
                    case "/start":
                        logger.info("Обработка команды /start от пользователя: {}", chatId);
                        menuService.sendMainMenu(chatId, this); // Оставляем видимой команду /start
                        break;
                    default:
                        logger.warn("Неизвестная команда: {}", messageText);
                        menuService.sendUnknownCommand(chatId, this);
                        try {
                            SendMessage message = new SendMessage();
                            message.setChatId(chatId);
                            message.setText("Команда не распознана. Попробуйте ещё раз.");
                            execute(message);
                        } catch (TelegramApiException e) {
                            logger.error("Ошибка отправки сообщения: {}", e.getMessage(), e);
                        }

                        deleteMessage(chatId, messageId);
                        break;
                }
            }

            // Обработка callback-запросов
            if (update.hasCallbackQuery()) {
                String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
                String callbackData = update.getCallbackQuery().getData();
                String username = (update.getMessage() != null && update.getMessage().getFrom().getUserName() != null)
                        ? update.getMessage().getFrom().getUserName()
                        : (update.getCallbackQuery() != null && update.getCallbackQuery().getFrom().getUserName() != null)
                        ? update.getCallbackQuery().getFrom().getUserName()
                        : "unknown";
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
                    case "reset_reservation":
                        vpnProvisionService.resetReservation(chatId);
                        vpnProvisionService.sendDeviceSelection(chatId); // Показываем меню выбора устройства
                        break;
                    case "device_phone":
                        menuService.sendOsSelectionMenu(chatId, this); // Показываем меню выбора ОС
                        break;
                    case "device_pc":
                        menuService.sendPcOsSelectionMenu(chatId, this); // Показываем меню выбора ОС для ПК
                        break;
                    case "instruction_ios":
                        instructionService.sendDeviceInstruction(chatId, "ios", this);
                        break;
                    case "instruction_android":
                        instructionService.sendDeviceInstruction(chatId, "android", this);
                        break;
                    case "instruction_windows":
                        instructionService.sendDeviceInstruction(chatId, "windows", this);
                        break;
                    case "instruction":
                        instructionService.sendDeviceInstructionMenu(chatId, this);
                        break;
                    case "os_android": // Логика для Android, например, сохранение выбора и переход к оплате
                        vpnProvisionService.handleOsSelection(chatId, "Android");
                        break;
                    case "os_ios": // Логика для iOS, например, сохранение выбора и переход к оплате
                        vpnProvisionService.handleOsSelection(chatId, "iOS");
                        break;
                    case "os_windows": // Логика для Windows
                        vpnProvisionService.handleOsSelection(chatId, "Windows");
                        break;
                    case "os_macos": // Логика для macOS
                        vpnProvisionService.handleOsSelection(chatId, "macOS");
                        break;
                    default:
                        if (callbackData.startsWith("show_qr_")) {
                            String[] parts = callbackData.split("_");
                            if (parts.length > 2) {
                                String clientId = parts[2];
                                qrCodeService.sendQrCode(chatId, this);
                            } else {
                                logger.warn("Некорректный формат callback-данных: {}", callbackData);
                            }
                        } else if (callbackData.startsWith("download_config_")) {
                            String clientId = callbackData.split("_")[2];
                            configFileService.sendConfigFile(chatId, this);
                        } else if (callbackData.startsWith("key_")) {
                            keyService.handleDeviceCallback(chatId, callbackData, this);
                        } else if (callbackData.startsWith("delete_config_")) {
                            String clientId = callbackData.split("_")[2];
                            keyService.deleteConfiguration(chatId, clientId, this);
                        } else if (callbackData.equals("plan_1_month")) {
                                vpnProvisionService.handlePlanSelection(chatId, "1 месяц", username);
                        } else if (callbackData.equals("plan_3_months")) {
                                vpnProvisionService.handlePlanSelection(chatId, "3 месяца", username);
                        } else if (callbackData.equals("plan_6_months")) {
                                vpnProvisionService.handlePlanSelection(chatId, "6 месяцев", username);
                        } else if (callbackData.equals("plan_1_year")) {
                                vpnProvisionService.handlePlanSelection(chatId, "1 год", username);
                        } else {
                            logger.warn("Неизвестные callback-данные: {}", callbackData);
                            menuService.sendUnknownCallback(chatId, this);
                        }
                        break;
                }
            }
        } catch (RuntimeException e) {
            logger.error("Ошибка выполнения: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Неизвестная ошибка: {}", e.getMessage(), e);
        }
    }

    // Метод для удаления сообщений
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
