package com.airline.delay.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "delays")
public class Delay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id")
    private Long flightId;

    private String reason;

    @Column(name = "delay_minutes")
    private Integer delayMinutes;

    public Delay() {}

    public Long getId() { return id; }

    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(Integer delayMinutes) { this.delayMinutes = delayMinutes; }
}
