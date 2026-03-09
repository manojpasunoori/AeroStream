package com.aerostream.analytics.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "route_configurations")
public class RouteConfigurationDocument {

    @Id
    private String route;

    private boolean enabled = true;

    private int onTimeThresholdMinutes = 15;

    public RouteConfigurationDocument() {
    }

    public RouteConfigurationDocument(String route, boolean enabled, int onTimeThresholdMinutes) {
        this.route = route;
        this.enabled = enabled;
        this.onTimeThresholdMinutes = onTimeThresholdMinutes;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOnTimeThresholdMinutes() {
        return onTimeThresholdMinutes;
    }

    public void setOnTimeThresholdMinutes(int onTimeThresholdMinutes) {
        this.onTimeThresholdMinutes = onTimeThresholdMinutes;
    }
}
