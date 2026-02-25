package com.airline.delay.service;

import com.airline.delay.entity.Delay;
import com.airline.delay.repo.DelayRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DelayService {
    private final DelayRepository repo;

    public DelayService(DelayRepository repo) {
        this.repo = repo;
    }

    public Delay create(Delay d) { return repo.save(d); }

    public List<Delay> list(Long flightId) {
        return (flightId == null) ? repo.findAll() : repo.findByFlightId(flightId);
    }
}
