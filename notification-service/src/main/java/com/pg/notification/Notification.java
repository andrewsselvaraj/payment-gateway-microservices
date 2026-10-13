package com.pg.notification;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record Notification(
        @NotBlank String type,
        @NotBlank String recipient,
        @NotBlank String message,
        String referenceId,
        Instant receivedAt) {

    Notification stamped() {
        return new Notification(type, recipient, message, referenceId, Instant.now());
    }
}
