package com.aerostream.analytics.repository;

import com.aerostream.analytics.model.RouteReliabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteReliabilityRepository extends JpaRepository<RouteReliabilityEntity, Long> {
    Optional<RouteReliabilityEntity> findByRoute(String route);
}
