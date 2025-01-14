package com.lokivpn.service;

import com.lokivpn.model.PaymentRecord;
import com.lokivpn.model.VpnClient;
import com.lokivpn.repository.PaymentRepository;
import com.lokivpn.repository.VpnClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Service
public class VpnConfigService {

    private static final Logger logger = LoggerFactory.getLogger(VpnConfigService.class);

    private final VpnClientRepository vpnClientRepository;

    public VpnConfigService(VpnClientRepository vpnClientRepository) {
        this.vpnClientRepository = vpnClientRepository;
    }

    public Optional<VpnClient> getAvailableVpnConfig() {
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

                logger.info("VPN конфигурация назначена клиенту: {}", vpnClient.getClientName());
                return Optional.of(vpnClient);
            } else {
                logger.warn("Нет доступных VPN конфигураций");
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Ошибка получения VPN конфигурации: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public List<VpnClient> getClientsForUser(Long userId) {
        return vpnClientRepository.findByUserId(userId);
    }

}
