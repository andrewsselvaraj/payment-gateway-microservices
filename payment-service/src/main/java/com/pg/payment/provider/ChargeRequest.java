package com.pg.payment.provider;

import java.math.BigDecimal;

public record ChargeRequest(
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        String cardNumber,
        String expiry,
        String cvv,
        String description) {
}
