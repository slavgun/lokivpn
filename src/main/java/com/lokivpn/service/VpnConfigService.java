package com.lokivpn.service;

import com.lokivpn.model.VpnClient;
import org.springframework.stereotype.Service;
import java.io.File;


@Service
public class VpnConfigService {

    public File getConfigFile(VpnClient vpnClient) {
        String remoteFilePath = vpnClient.getConfigFile();
        String localFilePath = "/tmp/" + vpnClient.getClientName() + ".conf";
        try {
            // Используем SCP для загрузки файла
            Process process = Runtime.getRuntime().exec(new String[]{
                    "scp", "root@46.29.234.231:" + remoteFilePath, localFilePath
            });
            process.waitFor();

            // Проверяем, что файл был скачан
            File file = new File(localFilePath);
            if (file.exists()) {
                return file;
            } else {
                throw new RuntimeException("Не удалось скачать файл конфигурации.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке файла конфигурации: " + e.getMessage(), e);
        }
    }

    public File getQrCodeFile(VpnClient vpnClient) {
        String remoteFilePath = vpnClient.getQrCodePath();
        String localFilePath = "/tmp/" + vpnClient.getClientName() + "_qrcode.png";
        try {
            // Используем SCP для загрузки файла
            Process process = Runtime.getRuntime().exec(new String[]{
                    "scp", "root@46.29.234.231:" + remoteFilePath, localFilePath
            });
            process.waitFor();

            // Проверяем, что файл был скачан
            File file = new File(localFilePath);
            if (file.exists()) {
                return file;
            } else {
                throw new RuntimeException("Не удалось скачать QR-код.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при загрузке QR-кода: " + e.getMessage(), e);
        }
    }
}
