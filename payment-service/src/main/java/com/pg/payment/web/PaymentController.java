package com.pg.payment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.payment.service.PaymentService;
import com.pg.payment.service.WebhookVerifier;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;
    private final WebhookVerifier verifier;
    private final ObjectMapper mapper;

    public PaymentController(PaymentService service, WebhookVerifier verifier, ObjectMapper mapper) {
        this.service = service;
        this.verifier = verifier;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                  @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = PaymentResponse.from(service.process(idempotencyKey, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentResponse.from(service.get(id));
    }

    @GetMapping
    public List<PaymentResponse> byOrder(@RequestParam String orderId) {
        return service.forOrder(orderId).stream().map(PaymentResponse::from).toList();
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable UUID id) {
        return PaymentResponse.from(service.refund(id));
    }

    @PostMapping("/webhooks")
    public ResponseEntity<PaymentResponse> webhook(@RequestHeader(value = "X-Signature", required = false) String signature,
                                                   @RequestBody String payload) throws IOException {
        if (!verifier.isValid(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        WebhookEvent event = mapper.readValue(payload, WebhookEvent.class);
        return ResponseEntity.ok(PaymentResponse.from(
                service.applyWebhook(event.providerReference(), event.status(), event.reason())));
    }
}
