# Flight Simulator Service

The simulator emits synthetic events into Kafka topic `flight.events.v1`.

## Scenarios

- `clear`: mostly on-time traffic.
- `storm`: heavy delays around hub airports.
- `congestion`: moderate delay spikes in constrained airports.
- `cascading`: chained delays likely to impact downstream routes.

## API

- `GET /health`
- `GET /scenarios`
- `POST /simulation/start` with `{"scenario":"storm"}`
- `POST /simulation/stop`

## Local Commands

- Start stack: `docker compose up -d --build`
- Start scenario:
  - `curl -X POST http://localhost:8091/simulation/start -H "Content-Type: application/json" -d "{\"scenario\":\"storm\"}"`
