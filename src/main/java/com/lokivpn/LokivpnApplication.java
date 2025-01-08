package com.lokivpn;

import com.lokivpn.bot.TelegramBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class LokivpnApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(LokivpnApplication.class, args);

        try {
            // Создаем Telegram API
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Получаем экземпляр TelegramBot из Spring Context
            TelegramBot telegramBot = context.getBean(TelegramBot.class);

            // Удаляем старый Webhook (если был настроен)
            telegramBot.execute(new DeleteWebhook());

            // Регистрируем бота для Long Polling
            botsApi.registerBot(telegramBot);

            System.out.println("Бот успешно запущен через Long Polling!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("Ошибка запуска бота: " + e.getMessage());
        }
    }
}

