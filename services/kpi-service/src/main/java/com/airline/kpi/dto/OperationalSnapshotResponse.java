package com.airline.kpi.dto;

public record OperationalSnapshotResponse(
        long totalFlights,
        long delayedFlights,
        double onTimePercentage,
        double averageDelayMinutes,
        int onTimeThresholdMinutes
) {
}
