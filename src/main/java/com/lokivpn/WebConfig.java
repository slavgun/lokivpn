package com.lokivpn;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Разрешить запросы на все пути
                .allowedOrigins("http://localhost:3000") // Разрешить запросы с вашего React-приложения
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Разрешить эти методы
                .allowedHeaders("*") // Разрешить любые заголовки
                .allowCredentials(true); // Разрешить отправку cookies, если необходимо
    }
}
