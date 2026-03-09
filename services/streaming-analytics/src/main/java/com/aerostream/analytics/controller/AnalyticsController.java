package com.aerostream.analytics.controller;

import com.aerostream.analytics.model.RouteConfigurationDocument;
import com.aerostream.analytics.model.RouteDelayAggregation;
import com.aerostream.analytics.realtime.RealtimeUpdateService;
import com.aerostream.analytics.service.RouteConfigurationService;
import com.aerostream.analytics.service.RouteReliabilityService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final RouteReliabilityService reliabilityService;
    private final RouteConfigurationService routeConfigurationService;
    private final RealtimeUpdateService realtimeUpdateService;

    public AnalyticsController(
            RouteReliabilityService reliabilityService,
            RouteConfigurationService routeConfigurationService,
            RealtimeUpdateService realtimeUpdateService
    ) {
        this.reliabilityService = reliabilityService;
        this.routeConfigurationService = routeConfigurationService;
        this.realtimeUpdateService = realtimeUpdateService;
    }

    @GetMapping("/routes/reliability")
    public Map<String, RouteDelayAggregation> latest() {
        return reliabilityService.latest();
    }

    @GetMapping("/routes/configuration")
    public List<RouteConfigurationDocument> routeConfiguration() {
        return routeConfigurationService.getAllConfigurations();
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return realtimeUpdateService.subscribe();
    }
}
