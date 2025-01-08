package com.lokivpn.bot.component;

import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.VpnClientRepository;
import com.lokivpn.service.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReservationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationScheduler.class);

    @Autowired
    private VpnClientRepository vpnClientRepository;

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    @Scheduled(cron = "0 */15 * * * ?") // Каждые 15 минут
    public void clearExpiredReservations() {
        List<VpnClient> expiredReservations = vpnClientRepository.findAllExpiredReservations(LocalDateTime.now());
        for (VpnClient client : expiredReservations) {
            // Сбрасываем резервацию клиента
            client.setChatId(null);
            client.setReservedUntil(null);
            client.setAssigned(false); // Обновляем поле

            vpnClientRepository.save(client); // Сохраняем изменения

            // Уведомляем пользователя, если у клиента был chatId
            if (client.getChatId() != null) {
                telegramNotificationService.sendNotification(
                        client.getChatId(),
                        "Ваша резервация истекла. Вы можете выбрать новый план, если хотите продолжить."
                );
            }
        }
        logger.info("Очистка истекших резерваций завершена. Удалено {} записей.", expiredReservations.size());
    }
}
