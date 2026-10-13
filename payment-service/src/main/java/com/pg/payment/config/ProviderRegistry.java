package com.pg.payment.config;

import com.pg.payment.provider.PaymentGatewayProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderRegistry {

    private final Map<String, PaymentGatewayProvider> providers;
    private final String defaultProvider;

    public ProviderRegistry(List<PaymentGatewayProvider> providers, PaymentProperties properties) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(PaymentGatewayProvider::name, Function.identity()));
        this.defaultProvider = properties.provider();
        if (!this.providers.containsKey(defaultProvider)) {
            throw new IllegalStateException("Configured payment.provider '" + defaultProvider
                    + "' not available. Known providers: " + this.providers.keySet());
        }
    }

    public PaymentGatewayProvider active() {
        return providers.get(defaultProvider);
    }

    public PaymentGatewayProvider byName(String name) {
        PaymentGatewayProvider provider = providers.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown payment provider: " + name);
        }
        return provider;
    }
}
