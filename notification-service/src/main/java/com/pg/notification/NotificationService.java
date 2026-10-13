package com.pg.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Dispatches notifications. This implementation logs and keeps the most recent messages in memory;
 * swap {@link #dispatch} for an email/SMS provider (Amazon SES/SNS, SendGrid, Twilio) in production.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RECENT = 200;

    private final Deque<Notification> recent = new ArrayDeque<>();

    public Notification send(Notification notification) {
        Notification stamped = notification.stamped();
        dispatch(stamped);
        synchronized (recent) {
            recent.addFirst(stamped);
            while (recent.size() > MAX_RECENT) {
                recent.removeLast();
            }
        }
        return stamped;
    }

    public List<Notification> recent() {
        synchronized (recent) {
            return List.copyOf(recent);
        }
    }

    private void dispatch(Notification n) {
        log.info("[{}] to {}: {}", n.type(), n.recipient(), n.message());
    }
}
