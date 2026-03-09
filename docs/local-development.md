# Local Development Setup

## Prerequisites

- Docker Desktop with Compose v2
- Java 17 and Maven (optional if running outside containers)
- Python 3.11+ (for simulator and ingestion service local runs)

## Start Platform

1. `cp .env.example .env`
2. `docker compose up -d --build`

## Local Services

- API Gateway: `http://localhost:8080`
- Kafka broker (host): `localhost:9092`
- Schema Registry: `http://localhost:8085`
- PostgreSQL: `localhost:5432`
- MongoDB: `localhost:27017`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Health Checks

- `curl http://localhost:8081/actuator/health`
- `curl http://localhost:8082/actuator/health`
- `curl http://localhost:8083/actuator/health`
- `curl http://localhost:8084/actuator/health`

## Notes

- Existing Java domain services currently use MySQL and remain available for backward compatibility.
- New event-driven analytics services introduced in later commits use Kafka, PostgreSQL, and MongoDB.
- Ingestion Service: `http://localhost:8090`
- Flight Simulator: `http://localhost:8091`
- Schema Registry (host): `http://localhost:8085`
- Streaming Analytics API: `http://localhost:8086/api/analytics/routes/reliability`
