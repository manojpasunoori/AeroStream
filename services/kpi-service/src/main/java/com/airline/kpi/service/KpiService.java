package com.airline.kpi.service;

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
}
