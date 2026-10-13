package com.pg.payment.provider;

import java.math.BigDecimal;

/**
 * Adapter for an external payment gateway (Stripe, Razorpay, Adyen, ...).
 * Add a new gateway by implementing this interface and registering it as a bean.
 */
public interface PaymentGatewayProvider {

    /** Unique provider name, matched against {@code payment.provider}. */
    String name();

    ChargeResult charge(ChargeRequest request);

    ChargeResult refund(String providerReference, BigDecimal amount, String currency);
}
