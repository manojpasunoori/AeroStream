from __future__ import annotations

import os
import time
from pathlib import Path
from typing import Optional

from confluent_kafka import SerializingProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import StringSerializer
from fastapi import FastAPI, Response
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Gauge, Histogram, generate_latest
from pydantic import BaseModel, Field


KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
SCHEMA_REGISTRY_URL = os.getenv("SCHEMA_REGISTRY_URL", "http://schema-registry:8081")
KAFKA_TOPIC = os.getenv("INGESTION_TOPIC", "flight-events")
SCHEMA_PATH = os.getenv("FLIGHT_EVENT_SCHEMA_PATH", "/schemas/flight-event.avsc")


FLIGHT_EVENTS_PROCESSED = Counter("flight_events_processed", "Count of flight events processed by ingestion service")
CONSUMER_LAG = Gauge("consumer_lag", "Synthetic consumer lag gauge")
SERVICE_LATENCY = Histogram("service_latency", "Latency of ingestion endpoint", buckets=(0.01, 0.05, 0.1, 0.25, 0.5, 1, 2, 5))
CONSUMER_LAG.set(0)


class FlightEvent(BaseModel):
    flightId: str
    airline: str
    origin: str
    destination: str
    timestamp: str
    delayMinutes: int
    status: str = Field(default="SCHEDULED")


app = FastAPI(title="AeroStream Ingestion Service", version="1.1.0")


def _resolve_schema_path() -> Path:
    configured = Path(SCHEMA_PATH)
    if configured.exists():
        return configured

    repo_root_schema = Path(__file__).resolve().parents[3] / "schemas" / "flight-event.avsc"
    if repo_root_schema.exists():
        return repo_root_schema

    # Keep original path if not found so runtime error points to configured location.
    return configured


def _producer() -> SerializingProducer:
    schema_registry_client = SchemaRegistryClient({"url": SCHEMA_REGISTRY_URL})
    schema_str = _resolve_schema_path().read_text(encoding="utf-8")
    avro_serializer = AvroSerializer(
        schema_registry_client=schema_registry_client,
        schema_str=schema_str,
        to_dict=lambda obj, ctx: obj,
    )
    return SerializingProducer(
        {
            "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
            "key.serializer": StringSerializer("utf_8"),
            "value.serializer": avro_serializer,
        }
    )


_producer_instance: Optional[SerializingProducer] = None


def _get_producer() -> SerializingProducer:
    global _producer_instance
    if _producer_instance is None:
        _producer_instance = _producer()
    return _producer_instance


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "topic": KAFKA_TOPIC}


@app.get("/metrics")
def metrics() -> Response:
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.post("/ingest/flight-events")
def ingest_flight_event(event: FlightEvent) -> dict:
    start = time.perf_counter()
    payload = event.model_dump()
    producer = _get_producer()
    producer.produce(topic=KAFKA_TOPIC, key=payload["flightId"], value=payload)
    producer.poll(0)
    producer.flush(5)
    FLIGHT_EVENTS_PROCESSED.inc()
    SERVICE_LATENCY.observe(time.perf_counter() - start)
    return {"status": "accepted", "flightId": payload["flightId"], "topic": KAFKA_TOPIC}
