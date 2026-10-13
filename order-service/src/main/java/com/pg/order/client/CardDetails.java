package com.pg.order.client;

import jakarta.validation.constraints.NotBlank;

public record CardDetails(@NotBlank String cardNumber, @NotBlank String expiry, @NotBlank String cvv) {

    @Override
    public String toString() {
        return "CardDetails[****]";
    }
}
