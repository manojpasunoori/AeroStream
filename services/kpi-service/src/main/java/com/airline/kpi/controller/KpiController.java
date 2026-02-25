package com.airline.kpi.controller;

import com.airline.kpi.dto.OperationalSnapshotResponse;
import com.airline.kpi.entity.KpiMetric;
import com.airline.kpi.service.KpiService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kpis")
@Validated
public class KpiController {

    private final KpiService service;

    public KpiController(KpiService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KpiMetric create(@RequestBody KpiMetric m) {
        return service.create(m);
    }

    @GetMapping
    public List<KpiMetric> list() {
        return service.list();
    }

    @GetMapping("/operational-snapshot")
    public OperationalSnapshotResponse operationalSnapshot(
            @RequestParam(defaultValue = "15")
            @Min(value = 0, message = "onTimeThresholdMinutes must be >= 0")
            @Max(value = 300, message = "onTimeThresholdMinutes must be <= 300")
            int onTimeThresholdMinutes
    ) {
        return service.getOperationalSnapshot(onTimeThresholdMinutes);
    }
}
