package com.airline.crew.repo;

import com.airline.crew.entity.CrewMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewRepository extends JpaRepository<CrewMember, Long> {
    List<CrewMember> findByFlightId(Long flightId);
}
