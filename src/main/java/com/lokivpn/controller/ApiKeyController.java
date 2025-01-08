package com.lokivpn.controller;

import com.lokivpn.model.ApiKey;
import com.lokivpn.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/api-keys")
public class ApiKeyController {
    @Autowired
    private ApiKeyService apiKeyService;

    @PostMapping("/create")
    public ResponseEntity<ApiKey> createApiKey(@RequestParam(required = false) String allowedIp,
                                               @RequestParam(required = false) Integer usageLimit) {
        ApiKey apiKey = apiKeyService.createApiKey(allowedIp, usageLimit);
        return ResponseEntity.ok(apiKey);
    }

    @GetMapping
    public ResponseEntity<List<ApiKey>> getAllApiKeys() {
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok("API-ключ удалён");
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateApiKey(@RequestParam String keyValue,
                                                  @RequestParam(required = false) String requestIp) {
        boolean isValid = apiKeyService.validateApiKey(keyValue, requestIp);
        return ResponseEntity.ok(isValid);
    }
}

