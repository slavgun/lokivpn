package com.lokivpn;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
public class WebhookConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebhookConfig.class);

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.webhook-url}")
    private String webhookUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @PostConstruct
    public void registerWebhook() {
        String url = "https://api.telegram.org/bot" + botToken + "/setWebhook?url=" + webhookUrl;

        try {
            RestTemplate restTemplate = restTemplate();
            String response = restTemplate.getForObject(url, String.class);
            logger.info("Webhook registered successfully: {}", response);
        } catch (Exception e) {
            logger.error("Failed to register webhook: {}", e.getMessage(), e);
        }
    }
}
