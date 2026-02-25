package com.airline.kpi.service;

import com.airline.kpi.dto.OperationalSnapshotResponse;
import com.airline.kpi.entity.KpiMetric;
import com.airline.kpi.repo.KpiRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KpiService {

    private final KpiRepository repo;

    public KpiService(KpiRepository repo) {
        this.repo = repo;
    }

    public KpiMetric create(KpiMetric m) {
        if (m.getCalculatedAt() == null) m.setCalculatedAt(LocalDateTime.now());
        return repo.save(m);
    }

    public List<KpiMetric> list() {
        return repo.findAll();
    }

    public OperationalSnapshotResponse getOperationalSnapshot(int onTimeThresholdMinutes) {
        long totalFlights = repo.countFlights();
        long delayedFlights = repo.countDelayedFlights(onTimeThresholdMinutes);

        double onTimePercentage = totalFlights == 0
                ? 100.0
                : ((double) (totalFlights - delayedFlights) / totalFlights) * 100.0;

        Double averageDelayMinutes = repo.averageDelayMinutes();
        double safeAverageDelayMinutes = averageDelayMinutes == null ? 0.0 : averageDelayMinutes;

        return new OperationalSnapshotResponse(
                totalFlights,
                delayedFlights,
                roundTo2Decimals(onTimePercentage),
                roundTo2Decimals(safeAverageDelayMinutes),
                onTimeThresholdMinutes
        );
    }

    private double roundTo2Decimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
