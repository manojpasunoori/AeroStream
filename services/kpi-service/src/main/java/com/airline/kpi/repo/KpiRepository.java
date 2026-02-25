package com.airline.kpi.repo;

import com.airline.kpi.entity.KpiMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiRepository extends JpaRepository<KpiMetric, Long> {
}
