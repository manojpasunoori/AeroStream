package com.airline.flight.controller;

import com.airline.flight.entity.Flight;
import com.airline.flight.service.FlightService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService service;

    public FlightController(FlightService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Flight create(@RequestBody Flight f) {
        return service.create(f);
    }

    @GetMapping
    public List<Flight> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Flight get(@PathVariable Long id) {
        return service.get(id);
    }
}
