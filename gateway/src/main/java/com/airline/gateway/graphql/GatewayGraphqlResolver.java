package com.airline.gateway.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Controller
public class GatewayGraphqlResolver {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public GatewayGraphqlResolver(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @QueryMapping
    public FlightStatus flightStatus(@Argument String flightId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("http://flight-service:8081/api/flights/{id}", flightId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(REQUEST_TIMEOUT);

            if (response == null) {
                return null;
            }

            return new FlightStatus(
                    stringValue(response.get("id")),
                    stringValue(response.get("flightNumber")),
                    stringValue(response.get("origin")),
                    stringValue(response.get("destination")),
                    stringValue(response.get("status"))
            );
        } catch (Exception ex) {
            return null;
        }
    }

    @QueryMapping
    public RoutePerformance routePerformance(@Argument String route) {
        Map<String, Object> aggregation = fetchRouteAggregation(route);
        if (aggregation == null) {
            return null;
        }

        int eventCount = intValue(aggregation.get("eventCount"));
        int delayedFlights = intValue(aggregation.get("delayedFlights"));
        int totalDelayMinutes = intValue(aggregation.get("totalDelayMinutes"));

        double averageDelay = eventCount == 0 ? 0.0 : (double) totalDelayMinutes / eventCount;
        double delayRatio = eventCount == 0 ? 0.0 : (double) delayedFlights / eventCount;
        double score = Math.max(0.0, Math.min(100.0, 100.0 - (averageDelay * 0.55) - (delayRatio * 40.0)));

        return new RoutePerformance(route, eventCount, delayedFlights, averageDelay, score);
    }

    @QueryMapping
    public Float reliabilityScore(@Argument String route) {
        RoutePerformance performance = routePerformance(route);
        return performance == null ? null : performance.reliabilityScore().floatValue();
    }

    private Map<String, Object> fetchRouteAggregation(String route) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("http://streaming-analytics:8086/api/analytics/routes/reliability")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || !response.containsKey(route)) {
                return null;
            }

            Object raw = response.get(route);
            if (raw instanceof Map<?, ?> mapValue) {
                return (Map<String, Object>) mapValue;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    public record FlightStatus(
            String flightId,
            String flightNumber,
            String origin,
            String destination,
            String status
    ) {
    }

    public record RoutePerformance(
            String route,
            Integer eventCount,
            Integer delayedFlights,
            Double averageDelay,
            Double reliabilityScore
    ) {
    }
}
