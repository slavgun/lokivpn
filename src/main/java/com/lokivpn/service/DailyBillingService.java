package com.lokivpn.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.lokivpn.model.User;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.repository.VpnClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DailyBillingService {

    private static final Logger logger = LoggerFactory.getLogger(DailyBillingService.class);

    private final UserRepository userRepository;
    private final VpnClientRepository vpnClientRepository;
    private final TelegramMessageSender telegramMessageSender;

    private final ConcurrentHashMap<String, String> serverPublicKeyCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public DailyBillingService(UserRepository userRepository,
                               VpnClientRepository vpnClientRepository,
                               TelegramMessageSender telegramMessageSender) {
        this.userRepository = userRepository;
        this.vpnClientRepository = vpnClientRepository;
        this.telegramMessageSender = telegramMessageSender;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void processDailyBalances() {
        logger.info("Начало процесса биллинга...");

        Pageable pageable = PageRequest.of(0, 100);
        Page<User> usersPage;

        do {
            usersPage = userRepository.findAll(pageable);
            processPage(usersPage);
            pageable = pageable.next();
        } while (usersPage.hasNext());

        logger.info("Процесс биллинга завершен.");
    }

    @Async
    public void processPage(Page<User> usersPage) {
        for (User user : usersPage.getContent()) {
            executorService.submit(() -> {
                try {
                    processUser(user);
                } catch (Exception e) {
                    logger.error("Ошибка обработки пользователя {}: {}", user.getId(), e.getMessage(), e);
                }
            });
        }
    }

    protected void processUser(User user) {
        Long chatId = user.getChatId();
        int clientsCount = vpnClientRepository.countByUserId(chatId);
        int dailyCharge = clientsCount * 5;

        logger.info("Обработка пользователя {}: баланс={}, клиенты={}, списание= {}",
                chatId, user.getBalance(), clientsCount, dailyCharge);

        if (clientsCount == 0) {
            logger.info("У пользователя {} нет клиентов, пропускаем.", chatId);
            return;
        }

        int balance = user.getBalance();

        if (balance >= dailyCharge) {
            handleSufficientBalance(user, dailyCharge);
        } else {
            handleInsufficientBalance(user);
        }
    }

    private void handleSufficientBalance(User user, int dailyCharge) {
        user.setBalance(user.getBalance() - dailyCharge);
        userRepository.save(user);
        logger.info("Успешно списано {} с пользователя {}. Новый баланс: {}", dailyCharge, user.getChatId(), user.getBalance());

        if (user.getBalance() <= dailyCharge * 3) {
            sendLowBalanceNotification(user.getChatId());
        }
    }

    private void handleInsufficientBalance(User user) {
        Long chatId = user.getChatId();

        List<VpnClient> clients = vpnClientRepository.findByUserId(chatId);

        vpnClientRepository.unassignClientsByUserId(chatId);
        logger.warn("Клиенты пользователя {} были отвязаны из-за недостатка средств.", chatId);

        regenerateVpnConfigs(clients);

        sendClientsRemovedNotification(chatId);
    }

    @Async
    public void regenerateVpnConfigs(List<VpnClient> clients) {
        logger.info("Начинается перегенерация конфигураций для {} клиентов", clients.size());
        for (VpnClient client : clients) {
            executorService.submit(() -> {
                try {
                    regenerateWireGuardConfig(client.getConfigFile(), client.getQrCodePath(), client.getClientName(), client.getServer());
                    logger.info("Конфигурация для клиента {} успешно перегенерирована.", client.getClientName());
                } catch (Exception e) {
                    logger.error("Ошибка при перегенерации конфигурации для клиента {}: {}", client.getClientName(), e.getMessage(), e);
                }
            });
        }
    }

    private void regenerateWireGuardConfig(String configPath, String qrCodePath, String clientName, String serverIp) throws Exception {
        System.out.println("Начинается процесс регенерации для клиента " + clientName + " на сервере " + serverIp);

        String clientPrivateKey = executeCommand(serverIp, "wg genkey");
        System.out.println("Сгенерирован приватный ключ для клиента " + clientName);

        String clientPublicKey = executeCommand(serverIp, "echo " + clientPrivateKey + " | wg pubkey");
        System.out.println("Сгенерирован публичный ключ для клиента " + clientName + ": " + clientPublicKey);

        String newConfigContent = String.format(
                """
                [Interface]
                PrivateKey = %s
                Address = 10.7.0.%d/32
                DNS = 8.8.8.8
    
                [Peer]
                PublicKey = %s
                Endpoint = %s:51820
                AllowedIPs = 0.0.0.0/0
                PersistentKeepalive = 25
                """,
                clientPrivateKey, getClientIpSuffix(clientName), getServerPublicKey(serverIp), serverIp
        );

        // Пишем конфигурацию на сервер
        writeConfigToRemoteServer(serverIp, configPath, newConfigContent);
        System.out.println("Конфигурация успешно записана для клиента " + clientName);
    }


    private String executeCommand(String host, String command) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession("root", host, 22);
        session.setPassword("Ckfduey3103"); // Убедитесь, что пароль верный
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream(), StandardCharsets.UTF_8));

        channel.connect();

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        channel.disconnect();
        session.disconnect();

        return output.toString().trim();
    }

    private void writeConfigToRemoteServer(String host, String remoteFilePath, String content) throws Exception {
        // Экранируем содержимое, чтобы избежать проблем с кавычками
        String escapedContent = content.replace("\"", "\\\"").replace("$", "\\$");

        // Формируем команду для записи содержимого в файл
        String command = String.format("echo \"%s\" > %s", escapedContent, remoteFilePath);

        // Выполняем команду на удаленном сервере
        String result = executeCommand(host, command);

        System.out.println("Результат записи на удаленный сервер: " + result);
    }


    private int getClientIpSuffix(String clientName) {
        return Integer.parseInt(clientName.split("_")[1]) + 1;
    }

    private String getServerPublicKey(String serverIp) throws Exception {
        if (serverPublicKeyCache.containsKey(serverIp)) {
            return serverPublicKeyCache.get(serverIp);
        }

        String publicKey = executeCommand(serverIp, "wg show wg0 public-key");
        serverPublicKeyCache.put(serverIp, publicKey);
        return publicKey;
    }

    @Async
    public void sendLowBalanceNotification(Long chatId) {
        telegramMessageSender.sendNotification(chatId, "\uD83D\uDCB3 У вас заканчиваются средства на балансе.");
    }

    @Async
    public void sendClientsRemovedNotification(Long chatId) {
        telegramMessageSender.sendNotification(chatId, "\u274C Ваши клиенты были удалены.");
    }
}
