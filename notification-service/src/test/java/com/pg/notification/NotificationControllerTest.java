package com.pg.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(NotificationService.class)
class NotificationControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void acceptsAndListsNotifications() throws Exception {
        mvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PAYMENT_CAPTURED\",\"recipient\":\"jane@example.com\",\"message\":\"Paid\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.receivedAt").exists());

        mvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PAYMENT_CAPTURED"));
    }

    @Test
    void rejectsIncompleteNotification() throws Exception {
        mvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }
}
