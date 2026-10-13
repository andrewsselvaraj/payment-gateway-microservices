package com.pg.payment.web;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String orderId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotBlank @Pattern(regexp = "[0-9 ]{12,23}|pm_[A-Za-z0-9_]+") String cardNumber,
        @Pattern(regexp = "(0[1-9]|1[0-2])/[0-9]{2}") String expiry,
        @Pattern(regexp = "[0-9]{3,4}") String cvv,
        @Email String customerEmail) {

    @Override
    public String toString() {
        // Never log card data
        return "PaymentRequest[orderId=%s, amount=%s %s]".formatted(orderId, amount, currency);
    }
}
