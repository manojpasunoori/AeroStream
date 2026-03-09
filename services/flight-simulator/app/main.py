from __future__ import annotations

import os
import threading
import time
from pathlib import Path
from typing import Optional

from confluent_kafka import SerializingProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import StringSerializer
from fastapi import FastAPI
from pydantic import BaseModel

from app.scenarios import SCENARIOS, build_flight_event


KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:29092")
SCHEMA_REGISTRY_URL = os.getenv("SCHEMA_REGISTRY_URL", "http://schema-registry:8081")
KAFKA_TOPIC = os.getenv("SIMULATOR_TOPIC", "flight.events.v1")
SCHEMA_PATH = os.getenv("FLIGHT_EVENT_SCHEMA_PATH", "/schemas/flight_event.avsc")
PUBLISH_INTERVAL_SECONDS = float(os.getenv("SIMULATOR_INTERVAL_SECONDS", "1.0"))


app = FastAPI(title="AeroStream Flight Simulator", version="1.1.0")


class SimulationRequest(BaseModel):
    scenario: str = "clear"


_running = False
_current_scenario = "clear"
_lock = threading.Lock()


def _load_schema(schema_path: str) -> str:
    return Path(schema_path).read_text(encoding="utf-8")


def _producer() -> SerializingProducer:
    schema_registry_client = SchemaRegistryClient({"url": SCHEMA_REGISTRY_URL})
    schema_str = _load_schema(SCHEMA_PATH)

    avro_serializer = AvroSerializer(
        schema_registry_client=schema_registry_client,
        schema_str=schema_str,
        to_dict=lambda obj, ctx: obj,
    )

    producer_config = {
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
        "key.serializer": StringSerializer("utf_8"),
        "value.serializer": avro_serializer,
    }
    return SerializingProducer(producer_config)


def _publish_loop() -> None:
    producer: Optional[SerializingProducer] = None
    while True:
        with _lock:
            if not _running:
                break
            scenario = _current_scenario

        try:
            if producer is None:
                producer = _producer()
            event = build_flight_event(scenario)
            producer.produce(topic=KAFKA_TOPIC, key=event["flight_id"], value=event)
            producer.poll(0)
        except Exception:
            # keep the simulator running to recover from transient broker outages
            time.sleep(2)
        time.sleep(PUBLISH_INTERVAL_SECONDS)

    if producer is not None:
        producer.flush(5)


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
