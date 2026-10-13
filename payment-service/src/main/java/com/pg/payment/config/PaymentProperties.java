package com.pg.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        String provider,
        String webhookSecret,
        String notificationServiceUrl,
        Stripe stripe) {

    public record Stripe(String baseUrl, String apiKey) {
    }
}
