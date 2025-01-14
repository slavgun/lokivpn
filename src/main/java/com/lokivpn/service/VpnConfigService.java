package com.lokivpn.service;

import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.VpnClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class VpnConfigService {

    private static final Logger logger = LoggerFactory.getLogger(VpnConfigService.class);

    private final VpnClientRepository vpnClientRepository;
    private final TelegramMessageSender telegramMessageSender;

    public VpnConfigService(VpnClientRepository vpnClientRepository,
                            TelegramMessageSender telegramMessageSender) {
        this.vpnClientRepository = vpnClientRepository;
        this.telegramMessageSender = telegramMessageSender;
    }

    public Optional<VpnClient> getAvailableVpnConfig(String chatId) {
        try {
            // Получение первой доступной конфигурации из базы данных
            Optional<VpnClient> optionalClient = vpnClientRepository.findFirstByAssignedFalse();

            if (optionalClient.isPresent()) {
                VpnClient vpnClient = optionalClient.get();

                // Логирование пути конфигурации
                logger.info("Selected config file path: {}", vpnClient.getConfigFile());

                // Пометка конфигурации как выданной
                vpnClient.setAssigned(true);
                vpnClientRepository.save(vpnClient);

                // Уведомление пользователя
                String message = String.format(
                        "Вы успешно назначили VPN клиент: %s.\n" +
                                "Скачать конфиг можно в личном кабинете, в разделе \"Мои клиенты\" " +
                                "по кнопке с названием полученного клиента: \"%s\".",
                        vpnClient.getClientName(),
                        vpnClient.getClientName()
                );

                // Отправка сообщения пользователю (замените sendMessage вашим методом отправки сообщения)
                telegramMessageSender.sendMessage(chatId, message);

                logger.info("VPN конфигурация назначена клиенту: {}", vpnClient.getClientName());
                return Optional.of(vpnClient);
            } else {
                // Уведомление пользователя об отсутствии конфигураций
                String noConfigsMessage = "К сожалению, в данный момент нет доступных VPN конфигураций. Попробуйте позже.";
                telegramMessageSender.sendMessage(chatId, noConfigsMessage);

                logger.warn("Нет доступных VPN конфигураций");
                return Optional.empty();
            }
        } catch (Exception e) {
            // Уведомление пользователя об ошибке
            String errorMessage = "Произошла ошибка при назначении VPN конфигурации. Пожалуйста, повторите позже.";
            telegramMessageSender.sendMessage(chatId, errorMessage);

            logger.error("Ошибка получения VPN конфигурации: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public List<Long> getAllUserIdsWithClients() {
        return vpnClientRepository.findAllUserIdsWithClients(); // Реализация в репозитории
    }


    public List<VpnClient> getClientsForUser(Long userId) {
        return vpnClientRepository.findByUserId(userId);
    }

}
