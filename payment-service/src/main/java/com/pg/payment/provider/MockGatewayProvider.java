package com.pg.payment.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sandbox gateway for local development and tests.
 * Card numbers ending in 0002 are declined, ending in 9995 fail with insufficient funds;
 * everything else succeeds.
 */
@Component
public class MockGatewayProvider implements PaymentGatewayProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        String card = request.cardNumber();
        if (card.endsWith("0002")) {
            return ChargeResult.failure("card_declined");
        }
        if (card.endsWith("9995")) {
            return ChargeResult.failure("insufficient_funds");
        }
        return ChargeResult.success("mock_ch_" + UUID.randomUUID());
    }

    @Override
    public ChargeResult refund(String providerReference, BigDecimal amount, String currency) {
        return ChargeResult.success("mock_re_" + UUID.randomUUID());
    }
}
