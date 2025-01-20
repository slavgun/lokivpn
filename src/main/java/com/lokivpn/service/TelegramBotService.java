package com.lokivpn.service;

import com.lokivpn.model.User;
import com.lokivpn.model.UserActionLog;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.UserActionLogRepository;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.repository.VpnClientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.io.File;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TelegramBotService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final TelegramMessageSender messageSender;
    private final PaymentService paymentService;
    private final VpnClientRepository vpnClientRepository;
    private final UserRepository userRepository;
    private final InstructionService instructionService;
    private final SupportService supportService;
    private final UserActionLogService userActionLogService;

    public TelegramBotService(TelegramMessageSender messageSender,
                              PaymentService paymentService,
                              VpnClientRepository vpnClientRepository,
                              UserRepository userRepository,
                              InstructionService instructionService,
                              SupportService supportService,
                              UserActionLogService userActionLogService) {
        this.messageSender = messageSender;
        this.paymentService = paymentService;
        this.vpnClientRepository = vpnClientRepository;
        this.userRepository = userRepository;
        this.instructionService = instructionService;
        this.supportService = supportService;
        this.userActionLogService = userActionLogService;
    }

    public void processUpdate(Update update) {
        logger.info("Processing update: {}", update);

        if (update.hasPreCheckoutQuery()) {
            paymentService.handlePreCheckoutQuery(update);
            return;
        }

        if (update.hasMessage() && update.getMessage().hasSuccessfulPayment()) {
            paymentService.handleSuccessfulPayment(update);
            return;
        }

        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText();
            logger.info("Received message from chat {}: {}", chatId, text);

            if (text.startsWith("/start")) {
                // Извлечение реферального кода, если он есть
                String[] parts = text.split(" ");
                String referralCode = parts.length > 1 ? parts[1] : null;

                org.telegram.telegrambots.meta.api.objects.User telegramUser = update.getMessage().getFrom();
                if (telegramUser == null) {
                    logger.error("Не удалось получить информацию о пользователе из сообщения.");
                    messageSender.sendMessage(chatId, "Ошибка: не удалось получить информацию о вашем аккаунте. Попробуйте позже.");
                    return;
                }

                Long chatIdLong = Long.parseLong(chatId);
                Optional<User> existingUser = userRepository.findByChatId(chatIdLong);

                if (existingUser.isEmpty()) {
                    // Новый пользователь
                    User newUser = new User();
                    newUser.setChatId(chatIdLong);
                    newUser.setUsername(telegramUser.getUserName() != null ? telegramUser.getUserName() : "unknown");
                    newUser.setBalance(0);

                    // Генерация реферальной ссылки
                    String referralLink = generateReferralLink(newUser);

                    // Обрабатываем реферальный код, если он есть
                    if (referralCode != null) {
                        paymentService.processReferral(newUser, referralCode, chatId);
                    }

                    userRepository.save(newUser);
                    logger.info("Новый пользователь добавлен: {}", newUser);

                    messageSender.sendMessage(chatId, "Добро пожаловать в LOKIVPN! Ваша реферальная ссылка: " + referralLink);
                } else {
                    logger.info("Пользователь с chatId {} уже существует.", chatId);
                }
                sendWelcomeMessage(chatId);
            } else {
                sendUnknownCommand(chatId);
            }
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String chatId = callbackQuery.getMessage().getChatId().toString();
        String data = callbackQuery.getData();
        Long userId = Long.parseLong(chatId);

        logger.info("Received callback query: {}", data);

        switch (data) {
            case "account":
                sendAccountInfo(chatId, userId);
                break;
            case "get_vpn":
                handleVpnRequest(chatId);
                break;
            case "my_clients":
                sendClientList(chatId, Long.parseLong(chatId)); // Используем chatId как userId
                break;
            case "pay":
                sendPaymentRequestMessage(chatId);
                break;
            case "instruction":
                instructionService.sendDeviceInstructionMenu(chatId);
                break;
            case "support":
                supportService.sendSupportInfo(chatId);
                break;
            case "main_menu":
                sendWelcomeMessage(chatId);
                break;
            case "referral":
                Optional<User> userOptional = userRepository.findByChatId(Long.parseLong(chatId));
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    sendReferralMenu(chatId, user);
                } else {
                    messageSender.sendMessage(chatId, "Пользователь не найден. Пожалуйста, используйте /start для регистрации.");
                }
                break;
            case "view_history": // Новый кейс для кнопки "История действий"
                handleViewHistory(chatId, userId);
                break;
            case "instruction_ios":
            case "instruction_android":
            case "instruction_windows":
            case "instruction_android_tv":
                // Убираем префикс "instruction_" перед передачей в метод
                String deviceType = data.replace("instruction_", "");
                instructionService.sendDeviceInstruction(chatId, deviceType);
                break;
            default:
                if (data.startsWith("client_")) {
                    Long clientId = Long.parseLong(data.split("_")[1]);
                    sendClientDetails(chatId, clientId);
                } else if (data.startsWith("download_config_")) {
                    Long clientId = Long.parseLong(data.split("_")[2]);
                    downloadConfig(chatId, clientId);
                } else if (data.startsWith("download_qr_")) {
                    Long clientId = Long.parseLong(data.split("_")[2]);
                    downloadQr(chatId, clientId);
                } else if (data.startsWith("pay_")) {
                    int amount = Integer.parseInt(data.split("_")[1]); // Получение суммы из callbackData
                    paymentService.initiatePayment(chatId, amount);
                } else if (data.startsWith("confirm_vpn_")) {
                    confirmVpnBinding(chatId);
                } else if (data.startsWith("cancel_vpn")) {
                    cancelVpnRequest(chatId);
                } else if (data.startsWith("unbind_client_")) {
                    Long clientId = Long.parseLong(data.split("_")[2]);
                    unbindClient(chatId, clientId);
                    return;
                } else {
                    logger.warn("Unknown callback data: {}", data);
                    messageSender.sendMessage(chatId, "❕Неизвестная команда.❕");
                }
                break;
        }
    }

//Получить VPN

    // Проверка баланса
    private void handleVpnRequest(String chatId) {
        Long chatIdLong = Long.parseLong(chatId);

        // Получаем пользователя по chatId
        User user = userRepository.findByChatId(chatIdLong)
                .orElseThrow(() -> new RuntimeException("Пользователь с chatId " + chatId + " не найден."));

        // Проверяем баланс пользователя
        int minimumBalance = 110; // Минимальный баланс для получения клиента
        if (user.getBalance() < minimumBalance) {
            InlineKeyboardMarkup markup = createPaymentButtons(); // Метод для создания кнопок оплаты
            messageSender.sendMessage(chatId,
                    "\uD83D\uDD12 У вас недостаточно средств для получения VPN-клиента. Пожалуйста, пополните баланс.", markup);
            return;
        }

        // Отправляем запрос на подтверждение
        InlineKeyboardMarkup markup = createConfirmationButtons(null); // Передаем null, так как клиент пока не нужен
        messageSender.sendMessage(chatId,
                "Подтвердите операцию ответом - подтвердить, если нажали случайно, нажмите - отмена", markup);
    }

    // Кнопки подтверждения
    private InlineKeyboardMarkup createConfirmationButtons(Long vpnClientId) {
        InlineKeyboardButton confirmButton = new InlineKeyboardButton("✅ Подтвердить");
        confirmButton.setCallbackData("confirm_vpn_" + vpnClientId);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton("❌ Отмена");
        cancelButton.setCallbackData("cancel_vpn");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(confirmButton, cancelButton)));
        return markup;
    }

    // Подтверждение и получение клиента
    private void confirmVpnBinding(String chatId) {
        Long chatIdLong = Long.parseLong(chatId);

        // Проверяем наличие пользователя
        User user = userRepository.findByChatId(chatIdLong)
                .orElseThrow(() -> new RuntimeException("Пользователь с chatId " + chatId + " не найден."));

        // Получаем первого доступного клиента
        VpnClient vpnClient = vpnClientRepository.findFirstByAssignedFalse()
                .orElseThrow(() -> new RuntimeException("Ошибка: нет доступных клиентов."));

        // Привязываем клиента к пользователю
        vpnClient.setAssigned(true);
        vpnClient.setUserId(chatIdLong);
        vpnClientRepository.save(vpnClient);

        // Отправляем уведомление
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton clientButton = new InlineKeyboardButton("\uD83D\uDD12 Мои VPN конфиги");
        clientButton.setCallbackData("my_clients");
        markup.setKeyboard(List.of(List.of(clientButton)));

        // Отправляем действие в лог
        userActionLogService.logAction(chatIdLong, "Конфиг создан", null);

        messageSender.sendMessage(chatId,
                String.format("✅Клиент '%s' успешно привязан. Скачать конфиг можно в личном кабинете.",
                        vpnClient.getClientName()), markup);
    }

    // Отмена операции
    private void cancelVpnRequest(String chatId) {
        messageSender.sendMessage(chatId, "Операция отменена.");
    }

    // Кнопки пополнения баланса
    private InlineKeyboardMarkup createPaymentButtons() {
        List<InlineKeyboardButton> row1 = List.of(
                createPaymentButton("150₽", "pay_150"), // изменения
                createPaymentButton("300₽", "pay_300"),
                createPaymentButton("600₽", "pay_600")
        );
        List<InlineKeyboardButton> row2 = List.of(
                createPaymentButton("900₽", "pay_900"),
                createPaymentButton("1200₽", "pay_1200"),
                createPaymentButton("1800₽", "pay_1800")
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row1, row2));
        return markup;
    }

    // Кнопка оплаты
    private InlineKeyboardButton createPaymentButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

//Приветственное сообщение

    private void sendWelcomeMessage(String chatId) {
        // Создаем кнопки
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton accountButton = new InlineKeyboardButton();
        accountButton.setText("\uD83C\uDFE0 Личный кабинет");
        accountButton.setCallbackData("account");

        InlineKeyboardButton vpnButton = new InlineKeyboardButton();
        vpnButton.setText("\uD83D\uDD11 Получить VPN");
        vpnButton.setCallbackData("get_vpn");

        InlineKeyboardButton instructionButton = new InlineKeyboardButton();
        instructionButton.setText("📘 Инструкция");
        instructionButton.setCallbackData("instruction");

        InlineKeyboardButton supportButton = new InlineKeyboardButton();
        supportButton.setText("\uD83D\uDCAC Поддержка");
        supportButton.setCallbackData("support");

        InlineKeyboardButton referralButton = new InlineKeyboardButton();
        referralButton.setText("👥 Реферальная система");
        referralButton.setCallbackData("referral");

        inlineKeyboardMarkup.setKeyboard(List.of(
                List.of(accountButton, vpnButton),
                List.of(instructionButton, referralButton),
                List.of(supportButton)
        ));

        // Путь к фото в ресурсах
        String photoPath = "images/loki.JPG"; // Путь внутри папки resources

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(photoPath)) {
            if (inputStream == null) {
                throw new NullPointerException("Фото не найдено в ресурсах: " + photoPath);
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(inputStream, "loki.JPG"));
            sendPhoto.setCaption("Добро пожаловать в LOKIVPN! Выберите действие ниже.");
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            messageSender.sendPhoto(sendPhoto);
        } catch (Exception e) {
            // Логирование и fallback
            logger.error("Не удалось загрузить изображение: {}", e.getMessage());
            SendMessage fallbackMessage = new SendMessage();
            fallbackMessage.setChatId(chatId);
            fallbackMessage.setText("Добро пожаловать в LOKIVPN! Выберите действие ниже.");
            fallbackMessage.setReplyMarkup(inlineKeyboardMarkup);
            messageSender.sendMessage(fallbackMessage);
        }
    }

//Личный кабинет

    // Главное меню кабинета
    private void sendAccountInfo(String chatId, Long userId) {
        int balance = getUserBalance(Long.parseLong(chatId)); // Преобразуем chatId в Long
        int clientCount = vpnClientRepository.countByUserId(userId);

        // Стоимость использования одного клиента в день
        int dailyCostPerClient = 5;
        int totalDailyCost = clientCount * dailyCostPerClient;

        // Расчет количества дней
        int daysAvailable = totalDailyCost > 0 ? balance / totalDailyCost : 0;

        // Текст для личного кабинета
        String accountInfo = String.format(
                "\uD83C\uDFE0 *Личный кабинет:*\n" +
                        "🔹 _Кол\\-во конфигов:_ *%d*\n" +
                        "💳 _Баланс:_ *%d RUB* \\(\\~%d дней\\)\n\n" +
                        "Тариф *150₽/мес* за 1 устройство\\.\n\n" +
                        "\uD83D\uDC6D _Пригласите друзей в наш сервис и получите *75₽*  на баланс за каждого друга\\. Ваши друзья так же получат *75₽*  на баланс\\!_",
                clientCount,
                balance,
                daysAvailable
        );

        // Кнопки меню
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton payButton = new InlineKeyboardButton();
        payButton.setText("\uD83E\uDD33 Пополнить баланс");
        payButton.setCallbackData("pay");

        InlineKeyboardButton myClientsButton = new InlineKeyboardButton();
        myClientsButton.setText("\uD83D\uDD12 Мои VPN конфиги");
        myClientsButton.setCallbackData("my_clients");

        InlineKeyboardButton historyButton = new InlineKeyboardButton();
        historyButton.setText("📜 История действий");
        historyButton.setCallbackData("view_history");

        /// Кнопка "Поделиться приглашением"
        InlineKeyboardButton inviteFriendButton = new InlineKeyboardButton();
        inviteFriendButton.setText("\uD83D\uDD17 Пригласить друга");
        // Установка текста с отступом и ссылкой
        inviteFriendButton.setSwitchInlineQuery("\n\n👇🏻 Присоединяйся к LOKIVPN по этой ссылке и получи 75 рублей на баланс: \n\nhttps://t.me/LokiVpnBot?start=" + generateReferralLink(getUserByChatId(chatId)));

        // Установка кнопок в разметку
        inlineKeyboardMarkup.setKeyboard(List.of(
                List.of(payButton),
                List.of(myClientsButton),
                List.of(historyButton),
                List.of(inviteFriendButton) // Кнопка "Пригласить друга"
        ));

        messageSender.sendMessage(chatId, accountInfo, inlineKeyboardMarkup, "MarkdownV2");
    }


    // Список клиентов
    public void sendClientList(String chatId, Long userId) {
        List<VpnClient> clients = getClientsForUser(userId);

        if (clients.isEmpty()) {
            sendMessage(chatId, "❕У вас нет подключенных клиентов.");
            return;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < clients.size(); i++) {
            VpnClient client = clients.get(i);
            InlineKeyboardButton clientButton = new InlineKeyboardButton();
            clientButton.setText("Конфиг⮚ #" + (i + 1)); // Форматируем текст кнопки
            clientButton.setCallbackData("client_" + client.getId()); // Используем оригинальный ID в callbackData
            rows.add(Collections.singletonList(clientButton));
        }

        inlineKeyboardMarkup.setKeyboard(rows);

        sendMessage(chatId, "\uD83D\uDCC2 Список конфигураций:", inlineKeyboardMarkup);
    }

    // Меню клиента
    public void sendClientDetails(String chatId, Long clientId) {
        Optional<VpnClient> optionalClient = vpnClientRepository.findById(clientId);

        if (optionalClient.isEmpty()) {
            sendMessage(chatId, "❕Клиент не найден.");
            return;
        }

        VpnClient client = optionalClient.get();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton configButton = new InlineKeyboardButton();
        configButton.setText("\uD83D\uDCC4 Скачать конфиг");
        configButton.setCallbackData("download_config_" + client.getId());

        InlineKeyboardButton qrButton = new InlineKeyboardButton();
        qrButton.setText("\uD83D\uDCF1 Показать QR код");
        qrButton.setCallbackData("download_qr_" + client.getId());

        InlineKeyboardButton unbindButton = new InlineKeyboardButton();
        unbindButton.setText("\uD83D\uDDD1\uFE0F Отвязать конфиг");
        unbindButton.setCallbackData("unbind_client_" + client.getId());

        rows.add(List.of(configButton, qrButton));
        rows.add(List.of(unbindButton));
        inlineKeyboardMarkup.setKeyboard(rows);

        sendMessage(chatId, "Выберите действие:", inlineKeyboardMarkup);
    }

    // История действий
    private void handleViewHistory(String chatId, Long userId) {
        List<UserActionLog> logs = userActionLogService.getLogsForUser(userId);

        // Предположим, что временная зона пользователя хранится в базе данных или задаётся вручную
        ZoneId userZoneId = ZoneId.of("Europe/Moscow"); // Пример для временной зоны Москвы

        StringBuilder historyMessage = new StringBuilder("Баланс: " + getUserBalance(userId) + "₽\n\n");
        historyMessage.append("```\n");
        historyMessage.append(String.format("%-20s %-5s %-30s\n", "ДАТА, ВРЕМЯ", "₽", "ТИП"));

        for (UserActionLog log : logs) {
            // Преобразуем время из UTC (или другой стандартной зоны) в зону пользователя
            ZonedDateTime userTime = log.getTimestamp().atZone(ZoneId.of("UTC")).withZoneSameInstant(userZoneId);

            historyMessage.append(String.format("%-20s %-5s %-30s\n",
                    userTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    log.getDetails(),
                    log.getActionType()
            ));
        }

        historyMessage.append("```");

        // Создаем объект SendMessage с правильной структурой
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(historyMessage.toString());
        sendMessage.setParseMode("MarkdownV2");

        // Отправляем сообщение через messageSender
        messageSender.sendMessage(sendMessage);
    }

    // Отвязка клиента
    private void unbindClient(String chatId, Long clientId) {
        // Находим клиента
        VpnClient client = vpnClientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Клиент не найден, хотя он отображается у пользователя."));

        // Убираем привязку клиента
        client.setAssigned(false);
        client.setUserId(null);
        vpnClientRepository.save(client);

        //Записываем действие в лог
        Long chatIdLong = Long.parseLong(chatId);
        userActionLogService.logAction(chatIdLong, "Конфиг отвязан", null);

        // Сообщаем об успехе
        sendMessage(chatId, String.format("✅ Клиент '%s' успешно отвязан.", client.getClientName()));
    }

    // Методы оплаты
    private void sendPaymentRequestMessage(String chatId) {
        String messageText = "\uD83E\uDEAA Выберите нужную сумму пополнения, нажмите на кнопку с выбранной суммой и произведите оплату.";

        InlineKeyboardMarkup markup = createPaymentButtons(); // Используем общий метод для создания кнопок

        messageSender.sendMessage(chatId, messageText, markup);
    }

    // Получение баланса
    public int getUserBalance(Long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::getBalance)
                .orElse(0); // Возвращаем 0, если пользователь не найден
    }

    // Метод для получения пользователя по chatId
    private User getUserByChatId(String chatId) {
        return userRepository.findByChatId(Long.parseLong(chatId))
                .orElseThrow(() -> new RuntimeException("Пользователь с chatId " + chatId + " не найден"));
    }

    // Получение списка клиентов у пользователя число
    public List<VpnClient> getClientsForUser(Long userId) {
        return vpnClientRepository.findByUserId(userId);
    }

    // Реферальная система

    public String generateReferralLink(User user) {
        if (user.getReferralCode() == null) {
            user.setReferralCode(UUID.randomUUID().toString());
            userRepository.save(user);
        }

        return "https://t.me/LokiVpnBot?start=" + user.getReferralCode();
    }

    private void sendReferralMenu(String chatId, User user) {
        // Кнопка для генерации ссылки и возможности поделиться
        InlineKeyboardButton shareButton = new InlineKeyboardButton();
        shareButton.setText("🔗 Пригласить друга");
        shareButton.setSwitchInlineQuery("\n\n👇🏻 Присоединяйся к LOKIVPN по этой ссылке и получи 75 рублей на баланс: \n\nhttps://t.me/LokiVpnBot?start=" + user.getReferralCode());

        // Создание разметки с кнопками
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(List.of(
                List.of(shareButton) // Новая кнопка для отправки приглашений
        ));

        // Текст сообщения со ссылкой
        String referralStats = getReferralStats(user.getId());
        messageSender.sendMessage(chatId, referralStats, inlineKeyboardMarkup);
    }

    public String getReferralStats(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        return "👥 Приглашённые пользователи: " + user.getReferredUsersCount() + "\n" +
                "💰 Бонусы за рефералов: " + user.getReferralBonus() + "₽\n" +
                "🔗 Ваша реферальная ссылка: https://t.me/LokiVpnBot?start=" + user.getReferralCode() + "\n\n" +
                "📢 Поделитесь своей ссылкой с друзьями, чтобы получить больше бонусов!";
    }


//Скачивание QR кода и конфига

    public void downloadConfig(String chatId, Long clientId) {
        Optional<VpnClient> optionalClient = vpnClientRepository.findById(clientId);

        if (optionalClient.isEmpty()) {
            messageSender.sendMessage(chatId, "❕Клиент не найден.");
            return;
        }

        VpnClient client = optionalClient.get();
        try {
            File configFile = messageSender.getConfigFile(client);
            messageSender.sendFile(chatId, configFile.getAbsolutePath(), "\uD83D\uDD12 Ваш конфиг");
        } catch (RuntimeException e) {
            messageSender.sendMessage(chatId, "❕Ошибка: " + e.getMessage());
        }
    }

    public void downloadQr(String chatId, Long clientId) {
        Optional<VpnClient> optionalClient = vpnClientRepository.findById(clientId);

        if (optionalClient.isEmpty()) {
            messageSender.sendMessage(chatId, "❕Клиент не найден.");
            return;
        }

        VpnClient client = optionalClient.get();
        try {
            File qrCodeFile = messageSender.getQrCodeFile(client);
            messageSender.sendFile(chatId, qrCodeFile.getAbsolutePath(), "\uD83D\uDDBC\uFE0F Ваш QR-код");
        } catch (RuntimeException e) {
            messageSender.sendMessage(chatId, "❕Ошибка: " + e.getMessage());
        }
    }

//Обработчики сообщений

    private void sendUnknownCommand(String chatId) {
        String message = "❕Неизвестная команда. Пожалуйста, выберите действие из меню.❕";
        messageSender.sendMessage(chatId, message);
    }


    private void sendMessage(String chatId, String text) {
        messageSender.sendMessage(chatId, text);
    }

    private void sendMessage(String chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(markup);

        messageSender.sendMessage(sendMessage);
    }
}