package com.lokivpn.service;

import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.PaymentRepository;
import com.lokivpn.repository.VpnClientRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DailyBillingService {

    private final PaymentRepository paymentRepository;
    private final VpnClientRepository vpnClientRepository;
    private final TelegramMessageSender telegramMessageSender;

    public DailyBillingService(PaymentRepository paymentRepository,
                               VpnClientRepository vpnClientRepository,
                               TelegramMessageSender telegramMessageSender) {
        this.paymentRepository = paymentRepository;
        this.vpnClientRepository = vpnClientRepository;
        this.telegramMessageSender = telegramMessageSender;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Запускается каждый день в полночь
    public void processDailyBalances() {
        List<Long> userIds = paymentRepository.findAllUserIds();

        for (Long userId : userIds) {
            int balance = paymentRepository.findBalanceByUserId(userId);
            List<VpnClient> clients = vpnClientRepository.findByUserId(userId);
            int dailyCharge = clients.size() * 5;

            if (balance >= dailyCharge) {
                // Списываем средства
                paymentRepository.updateBalanceByUserId(userId, balance - dailyCharge);

                // Если баланс меньше 3 дней, отправляем уведомление
                if (balance - dailyCharge <= dailyCharge * 3) {
                    telegramMessageSender.sendNotification(userId,
                            "У вас заканчиваются средства на балансе для оплаты клиента. " +
                                    "Пополните баланс в личном кабинете, если не совершите платеж в течение 3 дней, " +
                                    "клиенты будут удалены из вашего кабинета без возврата.");
                }
            } else {
                // Если баланс 0, отвязываем клиентов
                for (VpnClient client : clients) {
                    client.setAssigned(false);
                    client.setUserId(null);
                    vpnClientRepository.save(client);
                }
                telegramMessageSender.sendNotification(userId,
                        "Ваши клиенты были удалены из кабинета из-за отсутствия средств на оплату.");
            }
        }
    }
}


