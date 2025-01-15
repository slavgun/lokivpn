package com.lokivpn.service;

import com.lokivpn.model.User;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.repository.VpnClientRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DailyBillingService {

    private static final Logger logger = LoggerFactory.getLogger(DailyBillingService.class);

    private final UserRepository userRepository;
    private final VpnClientRepository vpnClientRepository;
    private final TelegramMessageSender telegramMessageSender;

    public DailyBillingService(UserRepository userRepository,
                               VpnClientRepository vpnClientRepository,
                               TelegramMessageSender telegramMessageSender) {
        this.userRepository = userRepository;
        this.vpnClientRepository = vpnClientRepository;
        this.telegramMessageSender = telegramMessageSender;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void processDailyBalances() {
        logger.info("Начало процесса биллинга...");

        Pageable pageable = PageRequest.of(0, 100);
        Page<User> usersPage;

        do {
            usersPage = userRepository.findAll(pageable);
            for (User user : usersPage.getContent()) {
                try {
                    processUser(user);
                } catch (Exception e) {
                    logger.error("Ошибка обработки пользователя {}: {}", user.getId(), e.getMessage());
                }
            }
            pageable = pageable.next();
        } while (usersPage.hasNext());

        logger.info("Процесс биллинга завершен.");
    }

    protected void processUser(User user) {
        // Используем chatId вместо id
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
        vpnClientRepository.unassignClientsByUserId(user.getChatId());
        logger.warn("Клиенты пользователя {} были удалены из-за недостатка средств.", user.getChatId());
        sendClientsRemovedNotification(user.getChatId());
    }

    @Async
    public void sendLowBalanceNotification(Long chatId) {
        try {
            telegramMessageSender.sendNotification(chatId,
                    "💳 У вас заканчиваются средства на балансе для оплаты клиентов.\n" +
                            "🔄 Пополните баланс в личном кабинете.\n" +
                            "🕒 Если не совершите платеж в течение 3 дней, клиенты будут удалены.");
        } catch (Exception e) {
            logger.error("Ошибка отправки уведомления о низком балансе для {}: {}", chatId, e.getMessage());
        }
    }

    @Async
    public void sendClientsRemovedNotification(Long chatId) {
        try {
            telegramMessageSender.sendNotification(chatId,
                    "❌ Ваши клиенты были удалены из кабинета из-за отсутствия средств на оплату.");
        } catch (Exception e) {
            logger.error("Ошибка отправки уведомления об удалении клиентов для {}: {}", chatId, e.getMessage());
        }
    }
}

