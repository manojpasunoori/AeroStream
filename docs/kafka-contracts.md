# Kafka Event Contracts

AeroStream uses Avro contracts with Schema Registry for topic `flight.events.v1`.

## Schema

- Location: `schemas/flight_event.avsc`
- Subject strategy: topic-value (`flight.events.v1-value`)
- Current version: `1.0.0`

## Producers

- `services/flight-simulator`: synthetic event source
- `services/ingestion-service`: API-based ingestion endpoint

Both services serialize events through Confluent Schema Registry.

## Compatibility Guidance

- Additive fields should be optional with defaults.
- Never remove required fields from existing event versions.
- Create new topic versions (`flight.events.v2`) for breaking changes.
