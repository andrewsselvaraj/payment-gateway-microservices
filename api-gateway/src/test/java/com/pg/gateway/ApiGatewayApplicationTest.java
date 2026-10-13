package com.pg.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApiGatewayApplicationTest {

    @Autowired
    RouteLocator routes;

    @Test
    void registersServiceRoutes() {
        var ids = routes.getRoutes().map(r -> r.getId()).collectList().block();
        assertThat(ids).contains("order-service", "payment-service", "notification-service");
    }
}
