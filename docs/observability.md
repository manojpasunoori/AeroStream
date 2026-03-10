# Observability

AeroStream ships with a complete observability stack for metrics and dashboards:

- Micrometer instrumentation in Spring Boot services
- Prometheus scraping and storage
- Grafana with provisioned dashboards
- OpenTelemetry Collector for trace ingestion

## Prometheus Endpoints

Spring services expose Prometheus metrics at:

- `http://localhost:8080/actuator/prometheus` (gateway)
- `http://localhost:8081/actuator/prometheus` (flight-service)
- `http://localhost:8082/actuator/prometheus` (crew-service)
- `http://localhost:8083/actuator/prometheus` (delay-service)
- `http://localhost:8084/actuator/prometheus` (kpi-service)
- `http://localhost:8086/actuator/prometheus` (streaming-analytics)

Python services expose metrics at:

- `http://localhost:8090/metrics` (ingestion-service)
- `http://localhost:8091/metrics` (flight-simulator)

## Key Metrics

- `flight_events_processed`
- `consumer_lag`
- `service_latency`
- JVM and HTTP server metrics from Micrometer/Actuator

## Access

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- OTel HTTP receiver: `http://localhost:4318`
