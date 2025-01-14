package com.lokivpn.service;

import com.lokivpn.model.User;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.PaymentRepository;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.repository.VpnClientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class TelegramBotService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final TelegramMessageSender messageSender;
    private final PaymentService paymentService;
    private final VpnConfigService vpnConfigService;
    private final VpnClientRepository vpnClientRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public TelegramBotService(TelegramMessageSender messageSender,
                              PaymentService paymentService,
                              VpnConfigService vpnConfigService,
                              VpnClientRepository vpnClientRepository,
                              PaymentRepository paymentRepository,
                              UserRepository userRepository) {
        this.messageSender = messageSender;
        this.paymentService = paymentService;
        this.vpnConfigService = vpnConfigService;
        this.vpnClientRepository = vpnClientRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    public void processUpdate(Update update) {
        logger.info("Processing update: {}", update);

        // Обработка PreCheckoutQuery
        if (update.hasPreCheckoutQuery()) {
            paymentService.handlePreCheckoutQuery(update);
            return;
        }

        // Обработка SuccessfulPayment
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

            switch (text) {
                case "/start":
                    org.telegram.telegrambots.meta.api.objects.User telegramUser = update.getMessage().getFrom(); // Telegram User
                    if (telegramUser == null) {
                        logger.error("Не удалось получить информацию о пользователе из сообщения.");
                        messageSender.sendMessage(chatId, "Ошибка: не удалось получить информацию о вашем аккаунте. Попробуйте позже.");
                        return;
                    }

                    // Проверяем, существует ли пользователь в таблице users
                    Long chatIdLong = Long.parseLong(chatId);
                    Optional<User> existingUser = userRepository.findByChatId(chatIdLong);
                    if (existingUser.isEmpty()) {
                        // Добавляем нового пользователя в таблицу users
                        User newUser = new User();
                        newUser.setChatId(chatIdLong);
                        newUser.setUsername(telegramUser.getUserName() != null ? telegramUser.getUserName() : "unknown");
                        newUser.setBalance(0); // Изначально баланс равен 0
                        newUser.setClientsCount(0); // Изначально клиентов нет

                        userRepository.save(newUser);
                        logger.info("Новый пользователь добавлен: {}", newUser);
                    } else {
                        logger.info("Пользователь с chatId {} уже существует.", chatId);
                    }

                    sendWelcomeMessage(chatId);
                    break;
                default:
                    sendUnknownCommand(chatId);
                    break;
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
                    Long vpnClientId = Long.parseLong(data.split("_")[2]); // Получение ID клиента
                    confirmVpnBinding(chatId, vpnClientId);
                } else if (data.startsWith("cancel_vpn")) {
                    cancelVpnRequest(chatId);
                } else if (data.startsWith("unbind_client_")) {
                    Long clientId = Long.parseLong(data.split("_")[2]);
                    unbindClient(chatId, clientId);
                    return;
                } else {
                    logger.warn("Unknown callback data: {}", data);
                    messageSender.sendMessage(chatId, "Неизвестная команда.");
                }
                break;
        }
    }

//Получить VPN

    private void handleVpnRequest(String chatId) {
        Long chatIdLong = Long.parseLong(chatId);

        // Получаем пользователя по chatId
        User user = userRepository.findByChatId(chatIdLong)
                .orElseThrow(() -> new RuntimeException("Пользователь с chatId " + chatId + " не найден."));

        // Проверяем доступность конфигурации
        Optional<VpnClient> availableConfig = vpnConfigService.getAvailableVpnConfig(chatId);

        if (availableConfig.isEmpty()) {
            messageSender.sendMessage(chatId, "Нет доступных VPN-конфигураций. Попробуйте позже.");
            return;
        }

        VpnClient vpnClient = availableConfig.get();

        // Привязываем клиента к пользователю
        vpnClient.setAssigned(true);
        vpnClient.setUserId(chatIdLong); // Используем chatId в качестве userId
        vpnClientRepository.save(vpnClient);

        // Увеличиваем счетчик клиентов у пользователя
        user.setClientsCount(user.getClientsCount() + 1);
        userRepository.save(user);

        // Отправляем уведомление
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton clientButton = new InlineKeyboardButton("Мои клиенты");
        clientButton.setCallbackData("my_clients");
        markup.setKeyboard(List.of(List.of(clientButton)));

        messageSender.sendMessage(chatId,
                String.format("Клиент '%s' успешно привязан. Скачать конфиг можно в личном кабинете.",
                        vpnClient.getClientName()),
                markup);
    }

    private void confirmVpnBinding(String chatId, Long vpnClientId) {
        Long chatIdLong = Long.parseLong(chatId);

        // Проверяем наличие конфигурации
        Optional<VpnClient> optionalClient = vpnClientRepository.findById(vpnClientId);

        if (optionalClient.isEmpty()) {
            messageSender.sendMessage(chatId, "Ошибка: не удалось найти VPN-конфигурацию. Попробуйте снова.");
            return;
        }

        VpnClient vpnClient = optionalClient.get();

        // Привязываем клиента к пользователю
        vpnClient.setAssigned(true);
        vpnClient.setUserId(chatIdLong); // Используем chatId в качестве userId
        vpnClientRepository.save(vpnClient);

        // Отправляем уведомление
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton clientButton = new InlineKeyboardButton("Мои клиенты");
        clientButton.setCallbackData("my_clients");
        markup.setKeyboard(List.of(List.of(clientButton)));

        messageSender.sendMessage(chatId,
                String.format("Клиент '%s' успешно привязан. Скачать конфиг можно по кнопке ниже или в Личном кабинете.",
                        vpnClient.getClientName()),
                markup);
    }


    private void cancelVpnRequest(String chatId) {
        messageSender.sendMessage(chatId, "Операция отменена.");
    }

    // Метод для создания кнопок подтверждения
    private InlineKeyboardMarkup createConfirmationButtons(Long vpnClientId) {
        InlineKeyboardButton confirmButton = new InlineKeyboardButton("✅ Подтвердить");
        confirmButton.setCallbackData("confirm_vpn_" + vpnClientId);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton("❌ Отмена");
        cancelButton.setCallbackData("cancel_vpn");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(confirmButton, cancelButton)));
        return markup;
    }


    // Метод для создания кнопок пополнения баланса
    private InlineKeyboardMarkup createPaymentButtons() {
        List<InlineKeyboardButton> row1 = List.of(
                createPaymentButton("110₽", "pay_110"), // изменения
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

    // Метод для создания кнопки оплаты
    private InlineKeyboardButton createPaymentButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

//Приветственное сообщение

    private void sendWelcomeMessage(String chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton accountButton = new InlineKeyboardButton();
        accountButton.setText("\uD83D\uDC64 Личный кабинет");
        accountButton.setCallbackData("account");

        InlineKeyboardButton vpnButton = new InlineKeyboardButton();
        vpnButton.setText("\uD83D\uDD11 Получить VPN");
        vpnButton.setCallbackData("get_vpn");

        inlineKeyboardMarkup.setKeyboard(List.of(
                List.of(accountButton, vpnButton)
        ));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Добро пожаловать в LOKIVPN! Выберите действие ниже.");
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        messageSender.sendMessage(sendMessage);
    }

//Личный кабинет

    private void sendAccountInfo(String chatId, Long userId) {
        int balance = getUserBalance(Long.parseLong(chatId)); // Преобразуем chatId в Long
        int clientCount = vpnClientRepository.countByUserId(userId);

        String accountInfo = String.format(
                "💼 *Ваш личный кабинет:*\n" +
                        "🔹 _Активные VPN:_ *%d*\n" +
                        "💰 _Баланс:_ *%d RUB*",
                clientCount,
                balance
        );

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton payButton = new InlineKeyboardButton();
        payButton.setText("💳 Пополнить баланс");
        payButton.setCallbackData("pay");

        InlineKeyboardButton myClientsButton = new InlineKeyboardButton();
        myClientsButton.setText("👥 Мои клиенты");
        myClientsButton.setCallbackData("my_clients");

        inlineKeyboardMarkup.setKeyboard(List.of(
                List.of(payButton),
                List.of(myClientsButton)
        ));

        messageSender.sendMessage(chatId, accountInfo, inlineKeyboardMarkup, "MarkdownV2");
    }


    public void sendClientList(String chatId, Long userId) {
        List<VpnClient> clients = vpnConfigService.getClientsForUser(userId);

        if (clients.isEmpty()) {
            sendMessage(chatId, "У вас нет подключенных клиентов.");
            return;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (VpnClient client : clients) {
            InlineKeyboardButton clientButton = new InlineKeyboardButton();
            clientButton.setText(client.getClientName());
            clientButton.setCallbackData("client_" + client.getId()); // Уникальный идентификатор
            rows.add(Collections.singletonList(clientButton));
        }

        inlineKeyboardMarkup.setKeyboard(rows);

        sendMessage(chatId, "Ваши клиенты:", inlineKeyboardMarkup);
    }

    public void sendClientDetails(String chatId, Long clientId) {
        Optional<VpnClient> optionalClient = vpnClientRepository.findById(clientId);

        if (optionalClient.isEmpty()) {
            sendMessage(chatId, "Клиент не найден.");
            return;
        }

        VpnClient client = optionalClient.get();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton configButton = new InlineKeyboardButton();
        configButton.setText("Скачать конфиг");
        configButton.setCallbackData("download_config_" + client.getId());

        InlineKeyboardButton qrButton = new InlineKeyboardButton();
        qrButton.setText("Скачать QR код");
        qrButton.setCallbackData("download_qr_" + client.getId());

        InlineKeyboardButton unbindButton = new InlineKeyboardButton();
        unbindButton.setText("Отвязать клиента");
        unbindButton.setCallbackData("unbind_client_" + client.getId());

        rows.add(List.of(configButton, qrButton));
        rows.add(List.of(unbindButton));
        inlineKeyboardMarkup.setKeyboard(rows);

        sendMessage(chatId, "Клиент: " + client.getClientName(), inlineKeyboardMarkup);
    }

    private void unbindClient(String chatId, Long clientId) {
        // Находим клиента
        VpnClient client = vpnClientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Клиент не найден, хотя он отображается у пользователя."));

        // Убираем привязку клиента
        client.setAssigned(false);
        client.setUserId(null);
        vpnClientRepository.save(client);

        // Уменьшаем счетчик клиентов
        userRepository.decrementClientCount(Long.parseLong(chatId));

        // Сообщаем об успехе
        sendMessage(chatId, String.format("Клиент '%s' успешно отвязан.", client.getClientName()));
    }


    private void sendPaymentRequestMessage(String chatId) {
        String messageText = "Выберите нужную сумму пополнения, нажмите на кнопку с выбранной суммой и произведите оплату.";

        InlineKeyboardMarkup markup = createPaymentButtons(); // Используем общий метод для создания кнопок

        messageSender.sendMessage(chatId, messageText, markup);
    }

    public int getUserBalance(Long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::getBalance)
                .orElse(0); // Возвращаем 0, если пользователь не найден
    }

    public void updateUserBalance(Long userId, int newBalance) {
        userRepository.updateBalanceByUserId(userId, newBalance);
        // Принудительный сброс контекста для актуализации данных
        entityManager.flush();
        entityManager.clear();
    }

//Скачивание QR кода и конфига

    public void downloadConfig(String chatId, Long clientId) {
        Optional<VpnClient> optionalClient = vpnClientRepository.findById(clientId);

        if (optionalClient.isEmpty()) {
            messageSender.sendMessage(chatId, "Клиент не найден.");
            return;
        }

        VpnClient client = optionalClient.get();
        try {
            File configFile = messageSender.getConfigFile(client);
            messageSender.sendFile(chatId, configFile.getAbsolutePath(), "Ваш конфигурационный файл");
        } catch (RuntimeException e) {
            messageSender.sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }

    public void downloadQr(String chatId, Long clientId) {
        Optional<VpnClient> optionalClient = vpnClientRepository.findById(clientId);

        if (optionalClient.isEmpty()) {
            messageSender.sendMessage(chatId, "Клиент не найден.");
            return;
        }

        VpnClient client = optionalClient.get();
        try {
            File qrCodeFile = messageSender.getQrCodeFile(client);
            messageSender.sendFile(chatId, qrCodeFile.getAbsolutePath(), "Ваш QR-код");
        } catch (RuntimeException e) {
            messageSender.sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }

//Обработчики сообщений

    private void sendUnknownCommand(String chatId) {
        String message = "Неизвестная команда. Пожалуйста, выберите действие из меню.";
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