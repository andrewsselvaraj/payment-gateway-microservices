package com.pg.payment.web;

import com.pg.payment.domain.PaymentStatus;

public record WebhookEvent(String providerReference, PaymentStatus status, String reason) {
}
