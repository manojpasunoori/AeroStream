package com.airline.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyFilter extends AbstractGatewayFilterFactory<ApiKeyFilter.Config> {

    public static class Config { }

    private final String apiKey;

    public ApiKeyFilter(@Value("${API_KEY:}") String apiKey) {
        super(Config.class);
        this.apiKey = apiKey;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // If API_KEY is not set, do not enforce (dev-friendly)
            if (apiKey == null || apiKey.isBlank()) {
                return chain.filter(exchange);
            }

            String provided = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
            if (provided == null || !provided.equals(apiKey)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }
}
