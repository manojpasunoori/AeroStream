package com.airline.kpi.controller;

import com.airline.kpi.entity.KpiMetric;
import com.airline.kpi.service.KpiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kpis")
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
}
