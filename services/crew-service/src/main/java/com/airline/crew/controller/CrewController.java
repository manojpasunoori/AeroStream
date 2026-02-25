package com.airline.crew.controller;

import com.airline.crew.entity.CrewMember;
import com.airline.crew.service.CrewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crew")
public class CrewController {

    private final CrewService service;

    public CrewController(CrewService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrewMember create(@RequestBody CrewMember c) {
        return service.create(c);
    }

    @GetMapping
    public List<CrewMember> list(@RequestParam(required = false) Long flightId) {
        return (flightId == null) ? service.list() : service.listByFlight(flightId);
    }
}
