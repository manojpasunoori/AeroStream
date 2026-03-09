package com.aerostream.analytics.service;

import com.aerostream.analytics.model.RouteDelayAggregation;
import com.aerostream.analytics.model.RouteReliabilityEntity;
import com.aerostream.analytics.repository.RouteReliabilityRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RouteReliabilityService {

    private final RouteReliabilityRepository repository;
    private final Map<String, RouteDelayAggregation> latestByRoute = new ConcurrentHashMap<>();

    public RouteReliabilityService(RouteReliabilityRepository repository) {
        this.repository = repository;
    }

    public void update(String route, RouteDelayAggregation aggregation) {
        latestByRoute.put(route, aggregation);

        RouteReliabilityEntity entity = repository.findByRoute(route)
                .orElseGet(() -> new RouteReliabilityEntity(route, 0, 0, 100));
        entity.setEventCount(aggregation.eventCount());
        entity.setAverageDelay(aggregation.averageDelay());
        entity.setReliabilityScore(aggregation.reliabilityScore());
        repository.save(entity);
    }

    public Map<String, RouteDelayAggregation> latest() {
        return latestByRoute;
    }
}
