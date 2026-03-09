from __future__ import annotations

import json
import os
import threading
import time
from typing import Optional

from fastapi import FastAPI
from kafka import KafkaProducer
from pydantic import BaseModel

from app.scenarios import SCENARIOS, build_flight_event


KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:29092")
KAFKA_TOPIC = os.getenv("SIMULATOR_TOPIC", "flight.events.v1")
PUBLISH_INTERVAL_SECONDS = float(os.getenv("SIMULATOR_INTERVAL_SECONDS", "1.0"))


app = FastAPI(title="AeroStream Flight Simulator", version="1.0.0")


class SimulationRequest(BaseModel):
    scenario: str = "clear"


_running = False
_current_scenario = "clear"
_lock = threading.Lock()


def _producer() -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda value: json.dumps(value).encode("utf-8"),
    )


def _publish_loop() -> None:
    producer: Optional[KafkaProducer] = None
    while True:
        with _lock:
            if not _running:
                break
            scenario = _current_scenario

        try:
            if producer is None:
                producer = _producer()
            event = build_flight_event(scenario)
            producer.send(KAFKA_TOPIC, event)
            producer.flush(timeout=2)
        except Exception:
            # keep the simulator running to recover from transient broker outages
            time.sleep(2)
        time.sleep(PUBLISH_INTERVAL_SECONDS)

    if producer is not None:
        producer.close()


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "running": _running, "scenario": _current_scenario}


@app.get("/scenarios")
def scenarios() -> dict:
    return {"scenarios": list(SCENARIOS.keys())}


@app.post("/simulation/start")
def start_simulation(request: SimulationRequest) -> dict:
    global _running, _current_scenario
    if request.scenario not in SCENARIOS:
        return {"status": "error", "message": f"unknown scenario '{request.scenario}'"}

    with _lock:
        _current_scenario = request.scenario
        if _running:
            return {"status": "running", "scenario": _current_scenario}
        _running = True

    thread = threading.Thread(target=_publish_loop, daemon=True)
    thread.start()
    return {"status": "started", "scenario": _current_scenario}


@app.post("/simulation/stop")
def stop_simulation() -> dict:
    global _running
    with _lock:
        _running = False
    return {"status": "stopped"}
