package com.pg.order.web;

import com.pg.order.domain.CustomerOrder;
import com.pg.order.service.OrderNotFoundException;
import com.pg.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrder create(@Valid @RequestBody CreateOrderRequest request) {
        return service.placeOrder(request);
    }

    @GetMapping("/{id}")
    public CustomerOrder get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public List<CustomerOrder> list() {
        return service.all();
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
