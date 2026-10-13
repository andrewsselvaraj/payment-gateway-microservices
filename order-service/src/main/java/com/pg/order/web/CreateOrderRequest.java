package com.pg.order.web;

import com.pg.order.client.CardDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank @Email String customerEmail,
        @NotBlank String item,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull @Valid CardDetails card) {
}
