package com.pg.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class Payment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String provider;

    private String providerReference;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    private String failureReason;

    /** Masked card number, e.g. ************4242. Full PAN is never stored. */
    private String maskedCard;

    private String customerEmail;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Payment() {
    }

    public Payment(String orderId, BigDecimal amount, String currency, String provider,
                   String idempotencyKey, String maskedCard, String customerEmail) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.idempotencyKey = idempotencyKey;
        this.maskedCard = maskedCard;
        this.customerEmail = customerEmail;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markCaptured(String providerReference) {
        this.status = PaymentStatus.CAPTURED;
        this.providerReference = providerReference;
        touch();
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        touch();
    }

    public void markRefunded() {
        if (status != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Only captured payments can be refunded, current status " + status);
        }
        this.status = PaymentStatus.REFUNDED;
        touch();
    }

    public void applyProviderStatus(PaymentStatus newStatus, String reason) {
        this.status = newStatus;
        if (reason != null) {
            this.failureReason = reason;
        }
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFailureReason() { return failureReason; }
    public String getMaskedCard() { return maskedCard; }
    public String getCustomerEmail() { return customerEmail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
