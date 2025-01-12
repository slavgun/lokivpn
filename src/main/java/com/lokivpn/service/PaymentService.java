package com.lokivpn.service;

import com.lokivpn.model.User;
import com.lokivpn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${yookassa.shop-id}")
    private String shopId;

    @Value("${yookassa.secret-key}")
    private String secretKey;

    public String createPaymentLink(Long chatId, String username, String plan) {
        try {
            int price = calculatePrice(plan);
            String orderId = UUID.randomUUID().toString();

            // Формирование тела запроса
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("amount", Map.of("value", String.format("%.2f", price / 100.0), "currency", "RUB"));
            paymentRequest.put("capture", true);
            paymentRequest.put("confirmation", Map.of("type", "redirect", "return_url", "https://your-return-url.com"));
            paymentRequest.put("description", "VPN Subscription: " + plan);

            // Добавляем информацию о чеке
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("items", List.of(Map.of(
                    "description", "VPN Subscription: " + plan,
                    "quantity", 1,
                    "amount", Map.of("value", String.format("%.2f", price / 100.0), "currency", "RUB"),
                    "vat_code", 4, // Без НДС
                    "payment_subject", "service", // Услуга
                    "payment_mode", "full_payment" // Полная оплата
            )));
            receipt.put("customer", Map.of(
                    "email", "slavgun@bk.ru" // Email пользователя
            ));
            paymentRequest.put("receipt", receipt);

            // Добавляем метаданные (chatId и username)
            Map<String, String> metadata = new HashMap<>();
            metadata.put("chat_id", String.valueOf(chatId));
            metadata.put("username", username);
            paymentRequest.put("metadata", metadata);

            // Отправка запроса
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(shopId, secretKey);
            headers.add("Idempotence-Key", UUID.randomUUID().toString());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentRequest, headers);

            String url = "https://api.yookassa.ru/v3/payments";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            // Получение URL подтверждения
            Map<String, String> confirmation = (Map<String, String>) response.get("confirmation");
            return confirmation.get("confirmation_url");

        } catch (Exception e) {
            logger.error("Error creating payment link: ", e);
            throw new RuntimeException("Error creating payment link", e);
        }
    }

    public int calculatePrice(String plan) {
        switch (plan) {
            case "1_month":
                return 200;
            case "3_months":
                return 500;
            case "6_months":
                return 900;
            case "1_year":
                return 1600;
            default:
                throw new IllegalArgumentException("Invalid plan: " + plan);
        }
    }
}
