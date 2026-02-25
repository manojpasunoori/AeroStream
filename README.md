✈️ Airline Operations Data Processing System

A fully containerized, production-style distributed backend system simulating airline operational analytics using Spring Boot microservices, API Gateway, MySQL, Docker, and observability tooling.

🏗 High-Level Architecture
Client
   ↓
API Gateway (8080)
   ↓
-------------------------------------------------
| flight-service  (8081) → flights table       |
| crew-service    (8082) → crew table          |
| delay-service   (8083) → delays table        |
| kpi-service     (8084) → kpi_metrics table   |
-------------------------------------------------
                    ↓
                 MySQL (3306)
🧠 System Overview

This system simulates airline operational workflows including:

Flight registration and lookup

Crew assignment per flight

Delay tracking and categorization

KPI storage and analytics

Operational KPI snapshot endpoint (on-time %, delayed flights, avg delay)

Distributed routing via API Gateway

Observability via Prometheus & Grafana

Correlation ID tracing across services

Centralized error handling

API documentation with Swagger

The architecture follows microservices best practices including:

Service isolation

Environment-based configuration

Docker container networking

Persistent storage via volumes

Health monitoring via Actuator

Metrics exposure via Prometheus

Structured logging with correlation IDs

🛠 Technology Stack
Backend

Java 17

Spring Boot 3.2.x

Spring Data JPA (Hibernate)

Spring Cloud Gateway

Spring Validation

Springdoc OpenAPI (Swagger)

Database

MySQL 8

Foreign key constraints

Persistent volume storage

Infrastructure

Docker & Docker Compose

WSL2 (Ubuntu 22.04)

Multi-stage Docker builds

Observability

Spring Boot Actuator

Prometheus

Grafana

Micrometer metrics

Correlation ID logging

📦 Project Structure
airline-ops-system/
├── docker-compose.yml
├── .env
├── gateway/
├── services/
│   ├── flight-service/
│   ├── crew-service/
│   ├── delay-service/
│   └── kpi-service/
├── infra/
│   ├── mysql/
│   ├── k8s/
│   └── observability/
├── load-test/
│   └── jmeter/
└── .github/workflows/
🚀 Getting Started
1️⃣ Clone Repository
git clone <your-repo-url>
cd airline-ops-system
2️⃣ Configure Environment

Create .env file:

MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=airline_ops
MYSQL_USER=airline_user
MYSQL_PASSWORD=airline_password
API_KEY=dev_key_change_me_1234567890
3️⃣ Build and Run
docker compose up -d --build
🔎 Service Endpoints
API Gateway

Base URL:

http://localhost:8080

All traffic flows through Gateway.

Example:

curl -H "X-API-KEY: <your_key>" \
     http://localhost:8080/api/flights
Individual Services
Service	Port
Gateway	8080
Flight Service	8081
Crew Service	8082
Delay Service	8083
KPI Service	8084
MySQL	3306
Prometheus	9090
Grafana	3000
📊 Observability
Actuator Health
curl http://localhost:8081/actuator/health
Prometheus Metrics
curl http://localhost:8081/actuator/prometheus

Prometheus UI:

http://localhost:9090
Grafana Dashboard
http://localhost:3000

(Default login: admin/admin)

📚 API Documentation (Swagger)

Each service exposes Swagger UI:

http://localhost:8081/swagger-ui/index.html
http://localhost:8082/swagger-ui/index.html
http://localhost:8083/swagger-ui/index.html
http://localhost:8084/swagger-ui/index.html

Operational snapshot example:

curl -H "X-API-KEY: <your_key>" \
     "http://localhost:8080/api/kpis/operational-snapshot?onTimeThresholdMinutes=15"

Threshold rules: `onTimeThresholdMinutes` must be between `0` and `300` (default `15`).

OpenAPI spec:

http://localhost:8081/v3/api-docs
🧾 Logging & Tracing

Correlation ID injected at Gateway

Propagated across all services

Included in log pattern

Enables request-level tracing across distributed system

Example log pattern:

INFO [cid=3b21d8f2-...] Flight created successfully
🔐 Security

API key enforced at Gateway layer

Header required:

X-API-KEY: <value_from_env>

Unauthorized requests return 401.

🧪 Load Testing

JMeter test plan available in:

load-test/jmeter/

Simulates concurrent API calls and system throughput.

☸ Kubernetes (Optional Deployment)

K8s manifests located under:

infra/k8s/

Includes:

Namespace

Deployments

Services

Ingress

MySQL config

Deploy via:

kubectl apply -f infra/k8s/
🧠 Engineering Concepts Applied

Distributed system architecture

API Gateway routing pattern

Container orchestration via Docker

Environment-based configuration

Structured error handling

Correlation ID tracing

Prometheus-based monitoring

Health endpoints

Persistent storage volumes

GitHub CI pipeline

Clean repo structure

📈 Future Enhancements

DTO separation layer

Inter-service communication via WebClient

Resilience4j (Circuit Breaker / Retry)

KPI aggregation logic

Redis caching

Distributed tracing (OpenTelemetry)

Authentication with JWT

🎯 Why This Project Matters

This system demonstrates:

Real-world microservices architecture

Production-grade containerization

Observability implementation

Clean API contracts

Infrastructure awareness

Backend engineering maturity

It is designed to simulate high-reliability airline backend systems where data consistency, fault tolerance, and traceability are critical.

👨‍💻 Author

Manoj Pasunoori
MS Information Systems – University of Texas at Arlington
Backend & Distributed Systems Engineer
