package com.aerostream.analytics.service;

import com.aerostream.analytics.model.RouteConfigurationDocument;
import com.aerostream.analytics.repository.RouteConfigurationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteConfigurationService {

    private static final int DEFAULT_ON_TIME_THRESHOLD_MINUTES = 15;

    private final RouteConfigurationRepository repository;

    public RouteConfigurationService(RouteConfigurationRepository repository) {
        this.repository = repository;
    }

    public RouteConfigurationDocument getRouteConfiguration(String route) {
        return repository.findById(route)
                .orElseGet(() -> new RouteConfigurationDocument(route, true, DEFAULT_ON_TIME_THRESHOLD_MINUTES));
    }

    public boolean isRouteEnabled(String route) {
        return getRouteConfiguration(route).isEnabled();
    }

    public List<RouteConfigurationDocument> getAllConfigurations() {
        return repository.findAll();
    }
}
