# Streaming Analytics Service

`services/streaming-analytics` runs Kafka Streams delay propagation analytics.

## What it does

- Consumes `flight.events.v1` (Avro)
- Creates 5-minute windowed route aggregations
- Computes route reliability scores (0-100)
- Publishes lightweight analytics to `route.delay.analytics.v1`
- Persists latest route score snapshots to PostgreSQL

## API

- `GET /api/analytics/routes/reliability`
