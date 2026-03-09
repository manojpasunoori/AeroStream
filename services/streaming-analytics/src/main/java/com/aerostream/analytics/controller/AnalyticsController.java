package com.aerostream.analytics.controller;

import com.aerostream.analytics.model.RouteDelayAggregation;
import com.aerostream.analytics.service.RouteReliabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final RouteReliabilityService reliabilityService;

    public AnalyticsController(RouteReliabilityService reliabilityService) {
        this.reliabilityService = reliabilityService;
    }

    @GetMapping("/routes/reliability")
    public Map<String, RouteDelayAggregation> latest() {
        return reliabilityService.latest();
    }
}
