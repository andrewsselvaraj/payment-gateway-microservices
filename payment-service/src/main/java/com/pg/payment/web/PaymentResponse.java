package com.pg.payment.web;

import com.pg.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String orderId,
        BigDecimal amount,
        String currency,
        String status,
        String provider,
        String providerReference,
        String maskedCard,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getCurrency(),
                p.getStatus().name(), p.getProvider(), p.getProviderReference(), p.getMaskedCard(),
                p.getFailureReason(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
