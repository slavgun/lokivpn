package com.lokivpn.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // Минимальное количество потоков в пуле
        executor.setMaxPoolSize(20); // Максимальное количество потоков в пуле
        executor.setQueueCapacity(500); // Размер очереди для задач, ожидающих выполнения
        executor.setThreadNamePrefix("AsyncExecutor-"); // Префикс для имени потоков
        executor.initialize();
        return executor;
    }
}

