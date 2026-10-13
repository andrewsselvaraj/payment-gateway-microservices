package com.pg.payment.client;

import com.pg.payment.config.PaymentProperties;
import com.pg.payment.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Best-effort, asynchronous notification; payment processing never fails because of it. */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient client;

    public NotificationClient(PaymentProperties properties, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.client = builder.baseUrl(properties.notificationServiceUrl()).requestFactory(factory).build();
    }

    @Async
    public void paymentEvent(Payment payment) {
        try {
            client.post()
                    .uri("/api/notifications")
                    .body(Map.of(
                            "type", "PAYMENT_" + payment.getStatus().name(),
                            "recipient", payment.getCustomerEmail() == null ? "unknown" : payment.getCustomerEmail(),
                            "message", "Payment %s for order %s is %s (%s %s)".formatted(
                                    payment.getId(), payment.getOrderId(), payment.getStatus(),
                                    payment.getAmount(), payment.getCurrency()),
                            "referenceId", payment.getId().toString()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send notification for payment {}: {}", payment.getId(), e.getMessage());
        }
    }
}
