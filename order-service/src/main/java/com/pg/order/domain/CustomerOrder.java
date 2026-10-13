package com.pg.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String item;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String paymentId;

    private String paymentFailureReason;

    @Column(nullable = false)
    private Instant createdAt;

    protected CustomerOrder() {
    }

    public CustomerOrder(String customerEmail, String item, BigDecimal amount, String currency) {
        this.id = UUID.randomUUID();
        this.customerEmail = customerEmail;
        this.item = item;
        this.amount = amount;
        this.currency = currency.toUpperCase();
        this.status = OrderStatus.CREATED;
        this.createdAt = Instant.now();
    }

    public void paymentSucceeded(String paymentId) {
        this.status = OrderStatus.PAID;
        this.paymentId = paymentId;
    }

    public void paymentFailed(String paymentId, String reason) {
        this.status = OrderStatus.PAYMENT_FAILED;
        this.paymentId = paymentId;
        this.paymentFailureReason = reason;
    }

    public void paymentPending(String reason) {
        this.status = OrderStatus.PAYMENT_PENDING;
        this.paymentFailureReason = reason;
    }

    public UUID getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public String getItem() { return item; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public String getPaymentId() { return paymentId; }
    public String getPaymentFailureReason() { return paymentFailureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
