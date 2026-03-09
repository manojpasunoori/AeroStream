package com.aerostream.analytics.service;

import com.aerostream.analytics.model.RouteDelayAggregation;
import com.aerostream.analytics.model.RouteReliabilityEntity;
import com.aerostream.analytics.realtime.RealtimeUpdateService;
import com.aerostream.analytics.repository.RouteReliabilityRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RouteReliabilityService {

    private final RouteReliabilityRepository repository;
    private final Counter flightEventsProcessed;
    private final AtomicLong consumerLag = new AtomicLong(0);
    private final Timer serviceLatency;
    private final RealtimeUpdateService realtimeUpdateService;
    private final Map<String, RouteDelayAggregation> latestByRoute = new ConcurrentHashMap<>();

    public RouteReliabilityService(
            RouteReliabilityRepository repository,
            MeterRegistry meterRegistry,
            RealtimeUpdateService realtimeUpdateService
    ) {
        this.repository = repository;
        this.realtimeUpdateService = realtimeUpdateService;
        this.flightEventsProcessed = meterRegistry.counter("flight_events_processed");
        this.serviceLatency = meterRegistry.timer("service_latency");
        Gauge.builder("consumer_lag", consumerLag, AtomicLong::get).register(meterRegistry);
    }

    public void update(String route, RouteDelayAggregation aggregation) {
        Timer.Sample sample = Timer.start();
        latestByRoute.put(route, aggregation);

        RouteReliabilityEntity entity = repository.findByRoute(route)
                .orElseGet(() -> new RouteReliabilityEntity(route, 0, 0, 100));
        entity.setEventCount(aggregation.eventCount());
        entity.setAverageDelay(aggregation.averageDelay());
        entity.setReliabilityScore(aggregation.reliabilityScore());
        repository.save(entity);

        flightEventsProcessed.increment();
        realtimeUpdateService.publish(route, aggregation);
        sample.stop(serviceLatency);
    }

    public Map<String, RouteDelayAggregation> latest() {
        return latestByRoute;
    }
}
