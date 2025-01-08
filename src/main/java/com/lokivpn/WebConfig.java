package com.lokivpn;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")       // Разрешаем доступ ко всем эндпоинтам /api/**
                .allowedOrigins("https://lokivpn.ru", "http://lokivpn.ru", "https://www.lokivpn.ru") // Тут домен, с которого приходят запросы
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);     // Если нужны куки и авторизация
    }
}



