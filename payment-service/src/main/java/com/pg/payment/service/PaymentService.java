package com.pg.payment.service;

import com.pg.payment.client.NotificationClient;
import com.pg.payment.config.ProviderRegistry;
import com.pg.payment.domain.Payment;
import com.pg.payment.domain.PaymentRepository;
import com.pg.payment.domain.PaymentStatus;
import com.pg.payment.provider.ChargeRequest;
import com.pg.payment.provider.ChargeResult;
import com.pg.payment.provider.PaymentGatewayProvider;
import com.pg.payment.web.PaymentRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final ProviderRegistry providers;
    private final NotificationClient notifications;
    private final MeterRegistry meters;

    public PaymentService(PaymentRepository repository, ProviderRegistry providers,
                          NotificationClient notifications, MeterRegistry meters) {
        this.repository = repository;
        this.providers = providers;
        this.notifications = notifications;
        this.meters = meters;
    }

    /**
     * Charges the customer through the active gateway. Repeating a request with the same
     * idempotency key returns the original payment instead of charging twice.
     */
    @Transactional
    public Payment process(String idempotencyKey, PaymentRequest request) {
        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay for key {}", idempotencyKey);
            return existing.get();
        }

        PaymentGatewayProvider provider = providers.active();
        Payment payment = repository.save(new Payment(
                request.orderId(), request.amount(), request.currency().toUpperCase(), provider.name(),
                idempotencyKey, mask(request.cardNumber()), request.customerEmail()));

        ChargeResult result = provider.charge(new ChargeRequest(
                idempotencyKey, request.amount(), request.currency(), request.cardNumber(),
                request.expiry(), request.cvv(), "Order " + request.orderId()));

        if (result.success()) {
            payment.markCaptured(result.providerReference());
        } else {
            payment.markFailed(result.failureReason());
        }
        meters.counter("payments.processed", "provider", provider.name(),
                "status", payment.getStatus().name()).increment();
        log.info("Payment {} for order {} -> {}", payment.getId(), payment.getOrderId(), payment.getStatus());
        notifications.paymentEvent(payment);
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Payment> forOrder(String orderId) {
        return repository.findByOrderId(orderId);
    }

    @Transactional
    public Payment refund(UUID id) {
        Payment payment = get(id);
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Only captured payments can be refunded, current status "
                    + payment.getStatus());
        }
        ChargeResult result = providers.byName(payment.getProvider())
                .refund(payment.getProviderReference(), payment.getAmount(), payment.getCurrency());
        if (!result.success()) {
            throw new IllegalStateException("Refund rejected by gateway: " + result.failureReason());
        }
        payment.markRefunded();
        meters.counter("payments.refunded", "provider", payment.getProvider()).increment();
        notifications.paymentEvent(payment);
        return payment;
    }

    /** Applies an asynchronous status update pushed by the gateway (e.g. chargeback, late capture). */
    @Transactional
    public Payment applyWebhook(String providerReference, PaymentStatus status, String reason) {
        Payment payment = repository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Unknown provider reference " + providerReference));
        if (payment.getStatus() != status) {
            payment.applyProviderStatus(status, reason);
            notifications.paymentEvent(payment);
        }
        return payment;
    }

    static String mask(String cardNumber) {
        String digits = cardNumber.replaceAll("\\s", "");
        if (digits.length() <= 4) {
            return digits;
        }
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }
}
