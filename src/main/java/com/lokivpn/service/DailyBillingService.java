package com.lokivpn.service;

import com.lokivpn.model.User;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.PaymentRepository;
import com.lokivpn.repository.UserRepository;
import com.lokivpn.repository.VpnClientRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DailyBillingService {

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
        Pageable pageable = PageRequest.of(0, 100); // Размер страницы: 100
        Page<User> usersPage;

        do {
            usersPage = userRepository.findUsersWithActiveClients(pageable);
            for (User user : usersPage.getContent()) {
                processUserAsync(user);
            }
            pageable = pageable.next(); // Переход к следующей странице
        } while (usersPage.hasNext());
    }


    private void processUser(User user) {
        int balance = user.getBalance();
        int clientsCount = user.getClientsCount();
        int dailyCharge = clientsCount * 5;

        if (clientsCount == 0) {
            // Если нет клиентов, пропускаем пользователя
            return;
        }

        if (balance >= dailyCharge) {
            handleSufficientBalance(user, dailyCharge);
        } else {
            handleInsufficientBalance(user);
        }
    }

    private void handleSufficientBalance(User user, int dailyCharge) {
        // Списываем средства
        user.setBalance(user.getBalance() - dailyCharge);
        userRepository.save(user);

        // Если баланс ниже трех дней расходов, отправляем уведомление
        if (user.getBalance() <= dailyCharge * 3) {
            sendLowBalanceNotification(user.getChatId());
        }
    }

    private void handleInsufficientBalance(User user) {
        // Bulk-операция для отвязывания всех клиентов пользователя
        vpnClientRepository.unassignClientsByUserId(user.getId());

        // Обнуляем количество клиентов
        user.setClientsCount(0);
        userRepository.save(user);

        // Отправляем уведомление пользователю
        sendClientsRemovedNotification(user.getChatId());
    }

    @Async
    public void processUserAsync(User user) {
        processUser(user);
    }


    @Async
    public void sendLowBalanceNotification(Long chatId) {
        telegramMessageSender.sendNotification(chatId,
                "У вас заканчиваются средства на балансе для оплаты клиентов. " +
                        "Пополните баланс в личном кабинете. Если не совершите платеж в течение 3 дней, клиенты будут удалены.");
    }

    @Async
    public void sendClientsRemovedNotification(Long chatId) {
        telegramMessageSender.sendNotification(chatId,
                "Ваши клиенты были удалены из кабинета из-за отсутствия средств на оплату.");
    }
}



