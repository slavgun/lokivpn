package com.lokivpn.service;

import com.lokivpn.model.PaymentRecord;
import com.lokivpn.model.User;
import com.lokivpn.repository.PaymentRepository;
import com.lokivpn.repository.UserRepository;
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
import java.util.List;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${telegram.provider-token}")
    private String providerToken;

    private final TelegramMessageSender messageSender;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final UserActionLogService userActionLogService;

    public PaymentService(TelegramMessageSender messageSender,
                          PaymentRepository paymentRepository,
                          UserRepository userRepository,
                          UserActionLogService userActionLogService) {
        this.messageSender = messageSender;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.userActionLogService = userActionLogService;
    }

    public void initiatePayment(String chatId, int amount) {
        if (amount < 108) {
            throw new IllegalArgumentException("Сумма платежа должна быть не менее 108 рублей.");
        }

        // Умножаем сумму на 100 для передачи в копейках
        int amountInKopecks = amount * 100;

        SendInvoice invoice = new SendInvoice();
        invoice.setChatId(chatId);
        invoice.setTitle("Пополнение баланса");
        invoice.setDescription("Пополнение баланса для получения VPN-конфигураций.");
        invoice.setPayload("balance_topup_" + amount);
        invoice.setProviderToken(providerToken);
        invoice.setCurrency("RUB");
        invoice.setPrices(List.of(new LabeledPrice("Пополнение баланса", amountInKopecks))); // сумма в копейках

        // Запросить email и передать данные для чека
        invoice.setNeedEmail(true);
        invoice.setSendEmailToProvider(true);

        // Формируем данные для чека с дополнительными полями
        String providerData = String.format("""
        {
            "receipt": {
                "items": [
                    {
                        "description": "Пополнение баланса",
                        "quantity": "1",
                        "amount": {
                            "value": "%.2f",
                            "currency": "RUB"
                        },
                        "vat_code": 1,
                        "payment_mode": "full_payment",
                        "payment_subject": "service"
                    }
                ]
            }
        }
        """, (double) amount); // сумма в рублях

        invoice.setProviderData(providerData);

        logger.info("Provider Data: {}", providerData); // Логирование
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
            Long chatIdLong = Long.parseLong(chatId);

            try {
                User user = userRepository.findByChatId(chatIdLong)
                        .orElseThrow(() -> new RuntimeException("Пользователь с chatId " + chatId + " не найден."));
                Long userId = user.getChatId();

                logger.info("Обработка платежа. ChatId: {}, UserId: {}", chatId, userId);

                // Сохранение информации о платеже
                PaymentRecord paymentRecord = new PaymentRecord();
                paymentRecord.setUserId(userId);
                paymentRecord.setAmount(payment.getTotalAmount()); // Сумма в копейках
                paymentRecord.setCurrency(payment.getCurrency());
                paymentRecord.setPaymentDate(LocalDateTime.now());
                paymentRecord.setProviderPaymentId(payment.getProviderPaymentChargeId());
                paymentRecord.setStatus("SUCCESS");

                logger.info("Сохраняем платёж: {}", paymentRecord);
                paymentRepository.save(paymentRecord);

                // Обновление баланса пользователя
                int currentBalance = getUserBalance(userId);
                int newBalance = currentBalance + payment.getTotalAmount() / 100; // Конвертация в рубли
                updateUserBalance(userId, newBalance);

                // Записываем действие
                userActionLogService.logAction(userId, "Пополнение", payment.getTotalAmount() / 100 + "₽");

                logger.info("Платёж успешно обработан, новый баланс: {}", newBalance);
                sendPaymentConfirmation(chatId); // Отправка сообщения о подтверждении платежа
            } catch (Exception e) {
                logger.error("Ошибка обработки платежа: {}", e.getMessage(), e);
                sendErrorMessage(chatId); // Отправка сообщения об ошибке
            }
        }
    }


    protected void processReferral(User newUser, String referralCode, String chatId) {
        User referrer = userRepository.findByReferralCode(referralCode);
        if (referrer != null) {
            // Связываем нового пользователя с реферером
            newUser.setReferredBy(referralCode);

            // Размер бонуса
            int bonus = 75;

            // Начисляем бонусы рефереру
            referrer.setReferralBonus(referrer.getReferralBonus() + bonus);
            referrer.setBalance(referrer.getBalance() + bonus); // Добавляем на баланс
            referrer.setReferredUsersCount(referrer.getReferredUsersCount() + 1);
            userRepository.save(referrer);

            // Начисляем бонус новому пользователю
            newUser.setReferralBonus(bonus);
            newUser.setBalance(newUser.getBalance() + bonus); // Добавляем на баланс

            // Записываем историю
            Long chatIdLong = Long.parseLong(chatId);
            userActionLogService.logAction(chatIdLong, "Реферальный бонус", bonus + "₽");


            // Отправляем уведомления
            messageSender.sendMessage(referrer.getChatId().toString(), "🎉 Вы получили 75₽ за приглашение нового пользователя! Бонус добавлен на ваш баланс.");
            messageSender.sendMessage(chatId, "🎉 Вам начислено 75₽ за регистрацию по реферальной ссылке! Бонус добавлен на ваш баланс.");
        }
    }


    public int getUserBalance(Long userId) {
        return userRepository.findById(userId)
                .map(User::getBalance)
                .orElse(0); // Если пользователь не найден, вернуть 0
    }

    private void updateUserBalance(Long userId, int newBalance) {
        userRepository.updateBalanceByUserId(userId, newBalance);
    }

    private void sendPaymentConfirmation(String chatId) {
        String message = "Ваш баланс пополнен.\uD83C\uDF89";
        messageSender.sendMessage(chatId, message);
    }

    private void sendErrorMessage(String chatId) {
        String message = "❌ Произошла ошибка при обработке вашего платежа. Пожалуйста, свяжитесь с поддержкой.";
        messageSender.sendMessage(chatId, message);
    }
}
