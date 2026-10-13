package com.pg.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PaymentClient {

    private final RestClient client;

    public PaymentClient(@Value("${services.payment-url}") String paymentUrl, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(10000);
        this.client = builder.baseUrl(paymentUrl).requestFactory(factory).build();
    }

    /** The order id is used as the idempotency key, so retries never double-charge an order. */
    public PaymentResult charge(String orderId, BigDecimal amount, String currency, CardDetails card, String email) {
        return client.post()
                .uri("/api/payments")
                .header("Idempotency-Key", "order-" + orderId)
                .body(Map.of(
                        "orderId", orderId,
                        "amount", amount,
                        "currency", currency,
                        "cardNumber", card.cardNumber(),
                        "expiry", card.expiry(),
                        "cvv", card.cvv(),
                        "customerEmail", email))
                .retrieve()
                .body(PaymentResult.class);
    }

    public record PaymentResult(String id, String status, String failureReason) {
    }
}
