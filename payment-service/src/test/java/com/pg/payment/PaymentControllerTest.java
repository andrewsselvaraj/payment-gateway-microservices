package com.pg.payment;

import com.pg.payment.client.NotificationClient;
import com.pg.payment.service.WebhookVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    WebhookVerifier verifier;

    @MockBean
    NotificationClient notifications;

    private static String body(String orderId, String card) {
        return """
                {"orderId":"%s","amount":49.99,"currency":"usd","cardNumber":"%s",
                 "expiry":"12/30","cvv":"123","customerEmail":"jane@example.com"}
                """.formatted(orderId, card);
    }

    @Test
    void capturesPaymentAndMasksCard() throws Exception {
        mvc.perform(post("/api/payments").header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body("o-1", "4242424242424242")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CAPTURED"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.maskedCard").value("************4242"));
    }

    @Test
    void declinedCardMarksPaymentFailed() throws Exception {
        mvc.perform(post("/api/payments").header("Idempotency-Key", "k-2")
                        .contentType(MediaType.APPLICATION_JSON).content(body("o-2", "4000000000000002")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("card_declined"));
    }

    @Test
    void sameIdempotencyKeyReturnsOriginalPayment() throws Exception {
        MvcResult first = mvc.perform(post("/api/payments").header("Idempotency-Key", "k-3")
                        .contentType(MediaType.APPLICATION_JSON).content(body("o-3", "4242424242424242")))
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/payments").header("Idempotency-Key", "k-3")
                        .contentType(MediaType.APPLICATION_JSON).content(body("o-3", "4242424242424242")))
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void refundsCapturedPaymentOnlyOnce() throws Exception {
        MvcResult created = mvc.perform(post("/api/payments").header("Idempotency-Key", "k-4")
                        .contentType(MediaType.APPLICATION_JSON).content(body("o-4", "4242424242424242")))
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/payments/{id}/refund", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
        mvc.perform(post("/api/payments/{id}/refund", id))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mvc.perform(post("/api/payments").header("Idempotency-Key", "k-5")
                        .contentType(MediaType.APPLICATION_JSON).content(body("o-5", "abc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookRequiresValidSignature() throws Exception {
        MvcResult created = mvc.perform(post("/api/payments").header("Idempotency-Key", "k-6")
                        .contentType(MediaType.APPLICATION_JSON).content(body("o-6", "4242424242424242")))
                .andReturn();
        String ref = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.providerReference");
        String payload = "{\"providerReference\":\"" + ref + "\",\"status\":\"FAILED\",\"reason\":\"chargeback\"}";

        mvc.perform(post("/api/payments/webhooks").header("X-Signature", "bad")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/payments/webhooks").header("X-Signature", verifier.sign(payload))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("chargeback"));
    }
}
