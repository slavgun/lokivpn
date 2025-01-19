package com.lokivpn.service;

import com.lokivpn.model.User;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.repository.VpnClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DailyBillingService {

    private static final Logger logger = LoggerFactory.getLogger(DailyBillingService.class);
    private static final int REQUEST_DELAY_SECONDS = 5; // Задержка между запросами

    private final UserRepository userRepository;
    private final VpnClientRepository vpnClientRepository;
    private final TelegramMessageSender telegramMessageSender;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    private RestTemplate restTemplate;

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

        logger.info("Обработка пользователя {}: баланс={}, клиенты={}, списание={}",
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

        // Удаляем только клиентов по одному, без перегенерации
        processClientsIndividually(clients);

        sendClientsRemovedNotification(chatId);
    }

    public void processClientsIndividually(List<VpnClient> clients) {
        logger.info("Начало обработки клиентов");

        // Группируем клиентов по серверам
        Map<String, List<VpnClient>> clientsByServer = clients.stream()
                .collect(Collectors.groupingBy(VpnClient::getServer));

        clientsByServer.forEach((server, serverClients) -> {
            try {
                String serverApiUrl = String.format("http://%s:8080/api/wireguard", server);

                // Удаление и перегенерация каждого клиента
                for (VpnClient client : serverClients) {
                    logger.info("Удаление клиента {} на сервере {}", client.getClientName(), server);
                    removeClient(serverApiUrl, client.getClientName());
                    addDelay(); // Задержка между запросами

                    logger.info("Перегенерация клиента {} на сервере {}", client.getClientName(), server);
                    regenerateClient(serverApiUrl, client.getClientName());
                    addDelay(); // Задержка между запросами
                }

                // Перезагрузка сервера после обработки всех клиентов
                logger.info("Перезапуск WireGuard на сервере {}", server);
                restartWireGuard(serverApiUrl);
            } catch (Exception e) {
                logger.error("Ошибка при обработке клиентов на сервере {}: {}", server, e.getMessage(), e);
            }
        });
    }

    private void removeClient(String serverApiUrl, String clientName) {
        try {
            String removeUrl = String.format("%s/remove-peer?clientName=%s", serverApiUrl, clientName);
            restTemplate.delete(removeUrl);
            logger.info("Клиент {} успешно удален через API сервера {}", clientName, serverApiUrl);
        } catch (Exception e) {
            logger.error("Ошибка удаления клиента {} через API сервера {}: {}", clientName, serverApiUrl, e.getMessage(), e);
        }
    }

    private void regenerateClient(String serverApiUrl, String clientName) {
        try {
            String regenerateUrl = String.format("%s/regenerate-client?clientName=%s", serverApiUrl, clientName);
            restTemplate.postForEntity(regenerateUrl, null, String.class);
            logger.info("Клиент {} успешно перегенерирован через API сервера {}", clientName, serverApiUrl);
        } catch (Exception e) {
            logger.error("Ошибка перегенерации клиента {} через API сервера {}: {}", clientName, serverApiUrl, e.getMessage(), e);
        }
    }


    private void restartWireGuard(String serverApiUrl) {
        try {
            String restartUrl = String.format("%s/restart", serverApiUrl);
            restTemplate.postForEntity(restartUrl, null, String.class);
            logger.info("WireGuard успешно перезапущен через API сервера {}", serverApiUrl);
        } catch (Exception e) {
            logger.error("Ошибка перезагрузки WireGuard через API сервера {}: {}", serverApiUrl, e.getMessage(), e);
        }
    }


    private void addDelay() {
        try {
            TimeUnit.SECONDS.sleep(REQUEST_DELAY_SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Ошибка задержки между запросами: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendLowBalanceNotification(Long chatId) {
        telegramMessageSender.sendNotification(chatId, "\uD83D\uDCB3 У вас заканчиваются средства на балансе.");
    }

    @Async
    public void sendClientsRemovedNotification(Long chatId) {
        telegramMessageSender.sendNotification(chatId, "❌ Ваши клиенты были удалены.");
    }
}

