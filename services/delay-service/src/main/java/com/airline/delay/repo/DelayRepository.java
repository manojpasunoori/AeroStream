package com.airline.delay.repo;

import com.airline.delay.entity.Delay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DelayRepository extends JpaRepository<Delay, Long> {
    List<Delay> findByFlightId(Long flightId);
}
