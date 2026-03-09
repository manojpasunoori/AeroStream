package com.aerostream.analytics.model;

public record RouteDelayAggregation(long eventCount, long totalDelayMinutes, long delayedFlights) {

    public RouteDelayAggregation add(int delayMinutes) {
        long delayedIncrement = delayMinutes > 15 ? 1 : 0;
        return new RouteDelayAggregation(eventCount + 1, totalDelayMinutes + delayMinutes, delayedFlights + delayedIncrement);
    }

    public double averageDelay() {
        if (eventCount == 0) {
            return 0;
        }
        return (double) totalDelayMinutes / eventCount;
    }

    public double reliabilityScore() {
        if (eventCount == 0) {
            return 100.0;
        }
        double delayRatio = (double) delayedFlights / eventCount;
        double score = 100.0 - (averageDelay() * 0.55) - (delayRatio * 40.0);
        return Math.max(0.0, Math.min(100.0, score));
    }
}
