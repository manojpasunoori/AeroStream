package com.airline.crew.service;

import com.airline.crew.entity.CrewMember;
import com.airline.crew.repo.CrewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrewService {

    private final CrewRepository repo;

    public CrewService(CrewRepository repo) {
        this.repo = repo;
    }

    public CrewMember create(CrewMember c) {
        return repo.save(c);
    }

    public List<CrewMember> list() {
        return repo.findAll();
    }

    public List<CrewMember> listByFlight(Long flightId) {
        return repo.findByFlightId(flightId);
    }
}
