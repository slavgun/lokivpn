package com.lokivpn.service;

import com.lokivpn.model.ApiKey;
import com.lokivpn.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class ApiKeyService {
    @Autowired
    private ApiKeyRepository apiKeyRepository;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public ApiKey createApiKey(String allowedIp, Integer usageLimit) {
        String key = generateApiKey();
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyValue(key);
        apiKey.setAllowedIp(allowedIp);
        apiKey.setUsageLimit(usageLimit);
        return apiKeyRepository.save(apiKey);
    }

    public Optional<ApiKey> findApiKey(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue);
    }

    public boolean validateApiKey(String keyValue, String requestIp) {
        Optional<ApiKey> apiKeyOptional = findApiKey(keyValue);
        if (apiKeyOptional.isEmpty()) {
            return false;
        }
        ApiKey apiKey = apiKeyOptional.get();
        if (!apiKey.isActive()) {
            return false;
        }
        if (apiKey.getAllowedIp() != null && !apiKey.getAllowedIp().equals(requestIp)) {
            return false;
        }
        return true;
    }

    public List<ApiKey> getAllApiKeys() {
        return apiKeyRepository.findAll();
    }

    public void deleteApiKey(Long id) {
        apiKeyRepository.deleteById(id);
    }

}
