package com.lokivpn.service;

import com.lokivpn.repository.VpnClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenService {

    @Autowired
    private Environment environment;

    @Autowired
    private VpnClientRepository vpnClientRepository;

    // Шифрование токена
    public String encrypt(String data) throws Exception {
        String serverKey = environment.getProperty("server.key");
        if (serverKey == null || serverKey.isEmpty()) {
            throw new RuntimeException("Фиксированный ключ сервера не настроен.");
        }
        SecretKeySpec secretKey = new SecretKeySpec(serverKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getUrlEncoder().encodeToString(encryptedData);
    }

    public String generateUniqueToken() {
        String token;
        do {
            token = generateShortUUID(); // Генерируем 12-значный UUID
        } while (vpnClientRepository.existsByEncryptedKey(token)); // Проверяем уникальность

        return token;
    }

    private String generateShortUUID() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", ""); // Убираем "-"
        return uuid.substring(0, 12); // Берём первые 12 символов
    }
}




