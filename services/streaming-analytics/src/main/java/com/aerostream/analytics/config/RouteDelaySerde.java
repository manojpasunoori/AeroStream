package com.aerostream.analytics.config;

import com.aerostream.analytics.model.RouteDelayAggregation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class RouteDelaySerde implements Serde<RouteDelayAggregation> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Serializer<RouteDelayAggregation> serializer() {
        return (topic, data) -> {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("failed to serialize RouteDelayAggregation", e);
            }
        };
    }

    @Override
    public Deserializer<RouteDelayAggregation> deserializer() {
        return (topic, data) -> {
            try {
                return mapper.readValue(data, RouteDelayAggregation.class);
            } catch (Exception e) {
                throw new RuntimeException("failed to deserialize RouteDelayAggregation", e);
            }
        };
    }
}
