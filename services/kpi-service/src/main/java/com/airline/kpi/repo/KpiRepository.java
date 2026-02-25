package com.airline.kpi.repo;

import com.airline.kpi.entity.KpiMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KpiRepository extends JpaRepository<KpiMetric, Long> {

    @Query(value = "SELECT COUNT(*) FROM flights", nativeQuery = true)
    long countFlights();

    @Query(value = "SELECT COUNT(DISTINCT flight_id) FROM delays WHERE delay_minutes > :threshold", nativeQuery = true)
    long countDelayedFlights(@Param("threshold") int threshold);

    @Query(value = "SELECT AVG(delay_minutes) FROM delays", nativeQuery = true)
    Double averageDelayMinutes();
}
