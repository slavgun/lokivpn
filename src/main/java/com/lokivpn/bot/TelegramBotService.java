package com.lokivpn.bot;

import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.VpnClientRepository;
import com.lokivpn.service.VpnConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Service
public class TelegramBotService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);

    private final VpnConfigService vpnConfigService;
    private final VpnClientRepository vpnClientRepository;
    private final AbsSender bot;

    public TelegramBotService(VpnConfigService vpnConfigService,
                              VpnClientRepository vpnClientRepository,
                              @Lazy AbsSender bot) {
        this.vpnConfigService = vpnConfigService;
        this.vpnClientRepository = vpnClientRepository;
        this.bot = bot;
    }

    public VpnClient assignClientToUser(String chatId, String username, String deviceType) {
        logger.info("Начало назначения VPN-клиента для chatId={}, username={}, deviceType={}", chatId, username, deviceType);

        // Проверка на превышение лимита устройств
        //int deviceCount = vpnClientRepository.countByChatIdAndDeviceType(chatId, deviceType);
        //if ((deviceType.equals("Phone") && deviceCount >= 3) || (deviceType.equals("PC") && deviceCount >= 2)) {
        //    throw new RuntimeException("Превышено количество доступных устройств для типа: " + deviceType);
        //}

        // Поиск доступного клиента
        VpnClient vpnClient = vpnClientRepository.findFirstByIsAssignedFalse()
                .orElseThrow(() -> new RuntimeException("Нет доступных конфигураций VPN"));

        // Назначение клиента
        vpnClient.setAssigned(true);
        vpnClient.setChatId(chatId);
        vpnClient.setUsername(username);
        vpnClient.setDeviceType(deviceType); // Установка типа устройства
        vpnClientRepository.save(vpnClient);

        return vpnClient;
    }

    public int countTotalDevices(String chatId) {
        return vpnClientRepository.countByChatId(chatId); // Предполагается, что такой метод уже существует
    }

    public void saveClient(VpnClient client) {
        vpnClientRepository.save(client);
    }


    public int countDevicesByType(String chatId, String deviceType) {
        return vpnClientRepository.countByChatIdAndDeviceType(chatId, deviceType);
    }

    public VpnClient getClientByChatId(String chatId) {
        return vpnClientRepository.findFirstByChatId(chatId)
                .orElseThrow(() -> new RuntimeException("VPN-клиент не найден для chatId: " + chatId));
    }

    public List<VpnClient> getClientsByChatId(String chatId) {
        List<VpnClient> clients = vpnClientRepository.findAllByChatId(chatId);
        if (clients.isEmpty()) {
            logger.info("Нет активных VPN-клиентов для chatId: {}", chatId);
        }
        return clients;
    }

    public VpnClient getClientById(String clientId) {
        return vpnClientRepository.findById(Long.parseLong(clientId))
                .orElseThrow(() -> new RuntimeException("VPN-клиент с ID " + clientId + " не найден"));
    }

    public String getLastActiveKey(String chatId) {
        Optional<VpnClient> vpnClient = vpnClientRepository.findFirstByChatId(chatId);
        if (vpnClient.isPresent()) {
            return vpnClient.get().getClientPublicKey(); // Если нужен публичный ключ клиента
        }
        return null; // Если клиента не найдено
    }

    public AbsSender getBot() {
        return bot;
    }

    public File getConfigFile(VpnClient vpnClient) {
        return vpnConfigService.getConfigFile(vpnClient);
    }

    public File getQrCodeFile(VpnClient vpnClient) {
        return vpnConfigService.getQrCodeFile(vpnClient);
    }
}


