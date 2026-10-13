package com.pg.payment.provider;

import com.pg.payment.config.PaymentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Stripe adapter using the PaymentIntents REST API.
 * Enabled with {@code payment.provider=stripe} and {@code payment.stripe.api-key}.
 * In production, card data should be tokenised client-side (Stripe.js / Elements) and only
 * the PaymentMethod id sent to this service; here the card number field carries that token.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
public class StripeGatewayProvider implements PaymentGatewayProvider {

    private final RestClient client;

    public StripeGatewayProvider(PaymentProperties properties, RestClient.Builder builder) {
        this.client = builder
                .baseUrl(properties.stripe().baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.stripe().apiKey())
                .build();
    }

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChargeResult charge(ChargeRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("amount", toMinorUnits(request.amount()));
        form.add("currency", request.currency().toLowerCase());
        form.add("payment_method", request.cardNumber());
        form.add("confirm", "true");
        form.add("description", request.description());
        form.add("automatic_payment_methods[enabled]", "true");
        form.add("automatic_payment_methods[allow_redirects]", "never");
        try {
            Map<String, Object> body = client.post()
                    .uri("/v1/payment_intents")
                    .header("Idempotency-Key", request.idempotencyKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (body != null && "succeeded".equals(body.get("status"))) {
                return ChargeResult.success((String) body.get("id"));
            }
            return ChargeResult.failure("stripe_status_" + (body == null ? "unknown" : body.get("status")));
        } catch (RestClientResponseException e) {
            return ChargeResult.failure("stripe_error_" + e.getStatusCode().value());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChargeResult refund(String providerReference, BigDecimal amount, String currency) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("payment_intent", providerReference);
        form.add("amount", toMinorUnits(amount));
        try {
            Map<String, Object> body = client.post()
                    .uri("/v1/refunds")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return ChargeResult.success(body == null ? null : (String) body.get("id"));
        } catch (RestClientResponseException e) {
            return ChargeResult.failure("stripe_error_" + e.getStatusCode().value());
        }
    }

    private static String toMinorUnits(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toPlainString();
    }
}
