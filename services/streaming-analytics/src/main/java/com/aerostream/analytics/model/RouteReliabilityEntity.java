package com.aerostream.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "route_reliability")
public class RouteReliabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String route;

    @Column(nullable = false)
    private long eventCount;

    @Column(nullable = false)
    private double averageDelay;

    @Column(nullable = false)
    private double reliabilityScore;

    public RouteReliabilityEntity() {
    }

    public RouteReliabilityEntity(String route, long eventCount, double averageDelay, double reliabilityScore) {
        this.route = route;
        this.eventCount = eventCount;
        this.averageDelay = averageDelay;
        this.reliabilityScore = reliabilityScore;
    }

    public Long getId() {
        return id;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public long getEventCount() {
        return eventCount;
    }

    public void setEventCount(long eventCount) {
        this.eventCount = eventCount;
    }

    public double getAverageDelay() {
        return averageDelay;
    }

    public void setAverageDelay(double averageDelay) {
        this.averageDelay = averageDelay;
    }

    public double getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(double reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
    }
}
