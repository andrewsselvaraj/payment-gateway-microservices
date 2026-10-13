package com.pg.order;

import com.pg.order.client.CardDetails;
import com.pg.order.client.PaymentClient;
import com.pg.order.client.PaymentClient.PaymentResult;
import com.pg.order.domain.OrderStatus;
import com.pg.order.service.OrderService;
import com.pg.order.web.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    OrderService service;

    @MockBean
    PaymentClient payments;

    private final CreateOrderRequest request = new CreateOrderRequest("jane@example.com", "Headphones",
            new BigDecimal("99.00"), "usd", new CardDetails("4242424242424242", "12/30", "123"));

    @Test
    void orderIsPaidWhenPaymentCaptured() {
        when(payments.charge(anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new PaymentResult("p-1", "CAPTURED", null));
        var order = service.placeOrder(request);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentId()).isEqualTo("p-1");
        assertThat(order.getCurrency()).isEqualTo("USD");
    }

    @Test
    void orderFailsWhenPaymentDeclined() {
        when(payments.charge(anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new PaymentResult("p-2", "FAILED", "card_declined"));
        var order = service.placeOrder(request);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.getPaymentFailureReason()).isEqualTo("card_declined");
    }

    @Test
    void orderPendingWhenPaymentServiceDown() {
        when(payments.charge(anyString(), any(), anyString(), any(), anyString()))
                .thenThrow(new ResourceAccessException("connection refused"));
        var order = service.placeOrder(request);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }
}
