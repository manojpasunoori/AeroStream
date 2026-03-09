package com.aerostream.analytics.realtime;

import com.aerostream.analytics.model.RouteDelayAggregation;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeUpdateService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));

        return emitter;
    }

    public void publish(String route, RouteDelayAggregation aggregation) {
        Map<String, Object> payload = Map.of(
                "route", route,
                "eventCount", aggregation.eventCount(),
                "averageDelay", aggregation.averageDelay(),
                "reliabilityScore", aggregation.reliabilityScore()
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("route-update").data(payload));
            } catch (IOException ex) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }
}
