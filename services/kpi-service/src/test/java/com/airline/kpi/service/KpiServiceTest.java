package com.airline.kpi.service;

import com.airline.kpi.dto.OperationalSnapshotResponse;
import com.airline.kpi.entity.KpiMetric;
import com.airline.kpi.repo.KpiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiServiceTest {

    @Mock
    private KpiRepository repo;

    private KpiService service;

    @BeforeEach
    void setUp() {
        service = new KpiService(repo);
    }

    @Test
    void create_setsCalculatedAt_whenMissing() {
        KpiMetric metric = new KpiMetric();
        metric.setMetricName("test_metric");
        metric.setMetricValue(10.0);

        when(repo.save(any(KpiMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KpiMetric saved = service.create(metric);

        assertThat(saved.getCalculatedAt()).isNotNull();
        verify(repo).save(metric);
    }

    @Test
    void operationalSnapshot_computesDerivedMetrics_withoutPersistingFromGetFlow() {
        when(repo.countFlights()).thenReturn(20L);
        when(repo.countDelayedFlights(15)).thenReturn(5L);
        when(repo.averageDelayMinutes()).thenReturn(12.3456);

        OperationalSnapshotResponse response = service.getOperationalSnapshot(15);

        assertThat(response.totalFlights()).isEqualTo(20L);
        assertThat(response.delayedFlights()).isEqualTo(5L);
        assertThat(response.onTimePercentage()).isEqualTo(75.0);
        assertThat(response.averageDelayMinutes()).isEqualTo(12.35);
        assertThat(response.onTimeThresholdMinutes()).isEqualTo(15);

        verify(repo, never()).save(any(KpiMetric.class));
    }

    @Test
    void operationalSnapshot_handlesNoFlights() {
        when(repo.countFlights()).thenReturn(0L);
        when(repo.countDelayedFlights(15)).thenReturn(0L);
        when(repo.averageDelayMinutes()).thenReturn(null);

        OperationalSnapshotResponse response = service.getOperationalSnapshot(15);

        assertThat(response.onTimePercentage()).isEqualTo(100.0);
        assertThat(response.averageDelayMinutes()).isEqualTo(0.0);
    }
}
