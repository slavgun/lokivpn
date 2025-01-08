package com.lokivpn.dto;

import java.time.LocalDateTime;

public class PaymentNotification {
    private Long clientId;
    private LocalDateTime paymentDate;

    // Getters and setters
    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }
}

