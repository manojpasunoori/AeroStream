from __future__ import annotations

import logging
import os
import random
import threading
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Literal, Optional

from confluent_kafka import SerializingProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import StringSerializer
from fastapi import FastAPI, Response
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Gauge, Histogram, generate_latest
from pydantic import BaseModel

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:29092")
SCHEMA_REGISTRY_URL = os.getenv("SCHEMA_REGISTRY_URL", "http://schema-registry:8081")
KAFKA_TOPIC = os.getenv("SIMULATOR_TOPIC", "flight-events")
SCHEMA_PATH = os.getenv("FLIGHT_EVENT_SCHEMA_PATH", "/schemas/flight-event.avsc")
PUBLISH_INTERVAL_SECONDS = float(os.getenv("SIMULATOR_INTERVAL_SECONDS", "5.0"))

FLIGHT_EVENTS_PROCESSED = Counter("flight_events_processed", "Count of flight events generated")
CONSUMER_LAG = Gauge("consumer_lag", "Synthetic consumer lag gauge")
SERVICE_LATENCY = Histogram("service_latency", "Latency of simulator event publication", buckets=(0.01, 0.05, 0.1, 0.25, 0.5, 1, 2, 5))
CONSUMER_LAG.set(0)

ROUTES = [
    ("DFW", "LAX"), ("DFW", "ORD"), ("DFW", "JFK"), ("DFW", "MIA"),
    ("LAX", "ORD"), ("LAX", "JFK"), ("LAX", "SEA"), ("ORD", "JFK"),
    ("JFK", "MIA"), ("SEA", "SFO"), ("ATL", "DFW"), ("ATL", "JFK"),
]
AIRLINES = ["American", "Delta", "United", "Southwest", "JetBlue"]

app = FastAPI(title="AeroStream Flight Data Connector", version="2.1.0")
LOGGER = logging.getLogger(__name__)

_running = False
_lock = threading.Lock()
_scenario: Literal["normal", "storm"] = "normal"
_event_counter = 0


class SimulationStartRequest(BaseModel):
    scenario: Literal["normal", "storm"] = "normal"


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
    return SerializingProducer({
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
        "key.serializer": StringSerializer("utf_8"),
        "value.serializer": avro_serializer,
    })


def _generate_synthetic_event(scenario: Literal["normal", "storm"]) -> dict:
    global _event_counter
    _event_counter += 1
    origin, destination = random.choice(ROUTES)
    airline = random.choice(AIRLINES)
    flight_id = f"{airline[:2].upper()}{random.randint(100, 999)}"

    if scenario == "storm":
        delay = random.randint(30, 120)
        if random.random() < 0.25:
            delay += random.randint(30, 90)
        status = "DELAYED"
    else:
        delay = random.randint(0, 15)
        status = "ON_TIME" if delay < 10 else "DELAYED"

    return {
        "flightId": flight_id,
        "airline": airline,
        "origin": origin,
        "destination": destination,
        "timestamp": datetime.now(tz=timezone.utc).isoformat(),
        "delayMinutes": delay,
        "status": status,
    }


def _publish_loop() -> None:
    producer: Optional[SerializingProducer] = None

    while True:
        with _lock:
            if not _running:
                break
            current_scenario = _scenario

        try:
            if producer is None:
                producer = _producer()

            start = time.perf_counter()
            batch_size = random.randint(3, 8)

            for _ in range(batch_size):
                event = _generate_synthetic_event(current_scenario)
                producer.produce(topic=KAFKA_TOPIC, key=event["flightId"], value=event)
                producer.poll(0)

            producer.flush(5)
            FLIGHT_EVENTS_PROCESSED.inc(batch_size)
            SERVICE_LATENCY.observe(time.perf_counter() - start)
            LOGGER.info("Published %s synthetic events (scenario=%s)", batch_size, current_scenario)

        except Exception as ex:
            LOGGER.error("Publishing cycle failed", exc_info=ex)
            producer = None
            time.sleep(2)

        time.sleep(PUBLISH_INTERVAL_SECONDS)

    if producer is not None:
        producer.flush(5)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "running": _running, "scenario": _scenario, "topic": KAFKA_TOPIC}


@app.get("/metrics")
def metrics() -> Response:
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.post("/simulation/start")
def start_simulation(request: Optional[SimulationStartRequest] = None) -> dict:
    global _running, _scenario
    with _lock:
        _scenario = request.scenario if request is not None else "normal"
        if _running:
            return {"status": "running", "scenario": _scenario}
        _running = True
    thread = threading.Thread(target=_publish_loop, daemon=True)
    thread.start()
    return {"status": "started", "scenario": _scenario}


@app.post("/simulation/stop")
def stop_simulation() -> dict:
    global _running
    with _lock:
        _running = False
    return {"status": "stopped"}