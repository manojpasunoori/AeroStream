package com.airline.delay.controller;

import com.airline.delay.entity.Delay;
import com.airline.delay.service.DelayService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delays")
public class DelayController {

    private final DelayService service;

    public DelayController(DelayService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Delay create(@RequestBody Delay d) {
        return service.create(d);
    }

    @GetMapping
    public List<Delay> list(@RequestParam(required = false) Long flightId) {
        return service.list(flightId);
    }
}
