# Realtime Dashboard

Dashboard service (`dashboard/`) subscribes to analytics updates via SSE.

## Stream Endpoint

- `GET /api/analytics/stream`
- Event name: `route-update`

## Run

- `docker compose up -d --build dashboard streaming-analytics gateway`
- Open `http://localhost:5173`
