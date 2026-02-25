package com.airline.flight.service;

import com.airline.flight.entity.Flight;
import com.airline.flight.repo.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightService {

    private final FlightRepository repo;

    public FlightService(FlightRepository repo) {
        this.repo = repo;
    }

    public Flight create(Flight f) {
        return repo.save(f);
    }

    public List<Flight> list() {
        return repo.findAll();
    }

    public Flight get(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Flight not found: " + id));
    }
}
