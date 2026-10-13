package com.pg.order.service;

import com.pg.order.client.PaymentClient;
import com.pg.order.domain.CustomerOrder;
import com.pg.order.domain.OrderRepository;
import com.pg.order.web.CreateOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final PaymentClient payments;

    public OrderService(OrderRepository repository, PaymentClient payments) {
        this.repository = repository;
        this.payments = payments;
    }

    public CustomerOrder placeOrder(CreateOrderRequest request) {
        CustomerOrder order = repository.save(new CustomerOrder(
                request.customerEmail(), request.item(), request.amount(), request.currency()));
        try {
            var result = payments.charge(order.getId().toString(), order.getAmount(), order.getCurrency(),
                    request.card(), order.getCustomerEmail());
            if ("CAPTURED".equals(result.status())) {
                order.paymentSucceeded(result.id());
            } else {
                order.paymentFailed(result.id(), result.failureReason());
            }
        } catch (RestClientException e) {
            // Payment outcome unknown; safe to retry because the order id is the idempotency key.
            log.warn("Payment service unavailable for order {}: {}", order.getId(), e.getMessage());
            order.paymentPending("payment_service_unavailable");
        }
        return repository.save(order);
    }

    public CustomerOrder get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<CustomerOrder> all() {
        return repository.findAll();
    }
}
