package com.lokivpn.service;

import com.lokivpn.model.PaymentRecord;
import com.lokivpn.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${telegram.provider-token}")
    private String providerToken;

    private final TelegramMessageSender messageSender;
    private final PaymentRepository paymentRepository;

    public PaymentService(TelegramMessageSender messageSender,
                          PaymentRepository paymentRepository) {
        this.messageSender = messageSender;
        this.paymentRepository = paymentRepository;
    }

    public void initiatePayment(String chatId, int amount) {
        SendInvoice invoice = new SendInvoice();
        invoice.setChatId(chatId);
        invoice.setTitle("Пополнение баланса");
        invoice.setDescription("Пополнение баланса для получения VPN-конфигураций.");
        invoice.setPayload("balance_topup_" + amount);
        invoice.setProviderToken(providerToken);
        invoice.setCurrency("RUB");
        invoice.setPrices(List.of(new LabeledPrice("Пополнение баланса", amount * 100))); // Умножаем на 100 для копеек.

        messageSender.sendInvoice(invoice);
        logger.info("Счёт на оплату отправлен для chatId: {} на сумму: {} RUB", chatId, amount);
    }

    public void handlePreCheckoutQuery(Update update) {
        if (update.hasPreCheckoutQuery()) {
            PreCheckoutQuery query = update.getPreCheckoutQuery();
            AnswerPreCheckoutQuery answer = new AnswerPreCheckoutQuery();
            answer.setOk(true);
            answer.setPreCheckoutQueryId(query.getId());

            messageSender.sendPreCheckoutQuery(answer);
            logger.info("PreCheckoutQuery успешно обработан для пользователя: {}", query.getFrom().getId());
        }
    }

    public void handleSuccessfulPayment(Update update) {
        if (update.getMessage().hasSuccessfulPayment()) {
            SuccessfulPayment payment = update.getMessage().getSuccessfulPayment();
            String chatId = update.getMessage().getChatId().toString();

            try {
                Long userId = Long.parseLong(chatId);

                // Сохранение информации о платеже в базу данных
                PaymentRecord paymentRecord = new PaymentRecord();
                paymentRecord.setUserId(userId);
                paymentRecord.setAmount(payment.getTotalAmount());
                paymentRecord.setCurrency(payment.getCurrency());
                paymentRecord.setPaymentDate(LocalDateTime.now());
                paymentRecord.setProviderPaymentId(payment.getProviderPaymentChargeId());
                paymentRecord.setStatus("SUCCESS");

                paymentRepository.save(paymentRecord);
                logger.info("Платёж успешно сохранён: {}", paymentRecord);

                sendPaymentConfirmation(chatId); // Отправка сообщения о подтверждении платежа
            } catch (NumberFormatException e) {
                logger.error("Ошибка преобразования chatId в Long: {}", chatId, e);
                sendErrorMessage(chatId); // Отправка сообщения об ошибке
            } catch (Exception e) {
                logger.error("Ошибка сохранения платежа: {}", e.getMessage(), e);
                sendErrorMessage(chatId); // Отправка сообщения об ошибке
            }
        }
    }


    private void sendPaymentConfirmation(String chatId) {
        String message = "Ваш платёж успешно обработан! Ваш баланс пополнен.";
        messageSender.sendMessage(chatId, message);
    }

    private void sendErrorMessage(String chatId) {
        String message = "Произошла ошибка при обработке вашего платежа. Пожалуйста, свяжитесь с поддержкой.";
        messageSender.sendMessage(chatId, message);
    }
}
