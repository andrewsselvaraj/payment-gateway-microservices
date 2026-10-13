package com.pg.notification;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Notification send(@Valid @RequestBody Notification notification) {
        return service.send(notification);
    }

    @GetMapping
    public List<Notification> recent() {
        return service.recent();
    }
}
