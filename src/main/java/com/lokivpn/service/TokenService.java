package com.lokivpn.service;

import com.lokivpn.repository.VpnClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
@Service
public class TokenService {

    @Autowired
    private Environment environment;

    @Autowired
    private VpnClientRepository vpnClientRepository;

    /**
     * Шифрует путь к конфигурации (config_file) без обрезки токена.
     */
    public String encrypt(String configPath) {
        try {
            String serverKey = getServerKey();
            SecretKeySpec secretKey = new SecretKeySpec(serverKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = cipher.doFinal(configPath.getBytes());

            // Теперь НЕ обрезаем строку, оставляем целиком
            return Base64.getUrlEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при шифровании пути", e);
        }
    }

    private String getServerKey() {
        String serverKey = environment.getProperty("server.key");
        if (serverKey == null || serverKey.isEmpty()) {
            throw new RuntimeException("Фиксированный ключ сервера не настроен.");
        }
        return serverKey;
    }
}

