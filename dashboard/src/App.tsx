import { useEffect, useMemo, useRef, useState } from "react";

type RouteSnapshot = {
  route: string;
  eventCount: number;
  averageDelay: number;
  reliabilityScore: number;
};

type FlightEventRow = {
  id: string;
  flightCode: string;
  route: string;
  origin: string;
  destination: string;
  delayMinutes: number;
  reliabilityScore: number;
  status: "On Time" | "Minor Delay" | "Major Delay";
  receivedAt: string;
};

type ConnectionState = "connecting" | "connected" | "reconnecting";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const LIVE_TABLE_LIMIT = 40;

const reliabilityBadge = (score: number) => {
  if (score >= 85) return "Excellent";
  if (score >= 70) return "Stable";
  return "At Risk";
};

const delayStatus = (delay: number): FlightEventRow["status"] => {
  if (delay < 10) return "On Time";
  if (delay < 30) return "Minor Delay";
  return "Major Delay";
};

const splitRoute = (route: string) => {
  const [origin = "N/A", destination = "N/A"] = route.split("->");
  return { origin, destination };
};

export function App() {
  const [routes, setRoutes] = useState<Record<string, RouteSnapshot>>({});
  const [liveFlights, setLiveFlights] = useState<FlightEventRow[]>([]);
  const [status, setStatus] = useState<ConnectionState>("connecting");
  const [lastUpdatedAt, setLastUpdatedAt] = useState<string | null>(null);
  const sequenceRef = useRef(0);

  useEffect(() => {
    let mounted = true;

    const loadInitialMetrics = async () => {
      try {
        const response = await fetch(`${API_BASE}/api/analytics/routes/reliability`);
        if (!response.ok) {
          return;
        }
        const payload = (await response.json()) as Record<string, Omit<RouteSnapshot, "route">>;
        if (!mounted) {
          return;
        }

        const normalized = Object.entries(payload).reduce<Record<string, RouteSnapshot>>(
          (acc, [route, metrics]) => {
            acc[route] = {
              route,
              eventCount: metrics.eventCount ?? 0,
              averageDelay: metrics.averageDelay ?? 0,
              reliabilityScore: metrics.reliabilityScore ?? 100
            };
            return acc;
          },
          {}
        );

        setRoutes(normalized);
      } catch {
        // The dashboard remains functional with live SSE even when the warmup request fails.
      }
    };

    void loadInitialMetrics();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    const source = new EventSource(`${API_BASE}/api/analytics/stream`);

    source.onopen = () => setStatus("connected");
    source.onerror = () => setStatus("reconnecting");
    source.addEventListener("route-update", (evt: MessageEvent) => {
      const payload = JSON.parse(evt.data) as RouteSnapshot;
      const receivedDate = new Date();
      const receivedAt = receivedDate.toLocaleTimeString();
      const { origin, destination } = splitRoute(payload.route);
      const currentSequence = ++sequenceRef.current;

      setRoutes((prev) => ({ ...prev, [payload.route]: payload }));
      setLiveFlights((prev) => {
        const event: FlightEventRow = {
          id: `${payload.route}-${currentSequence}`,
          flightCode: `FL-${currentSequence.toString().padStart(4, "0")}`,
          route: payload.route,
          origin,
          destination,
          delayMinutes: payload.averageDelay,
          reliabilityScore: payload.reliabilityScore,
          status: delayStatus(payload.averageDelay),
          receivedAt
        };
        return [event, ...prev].slice(0, LIVE_TABLE_LIMIT);
      });
      setLastUpdatedAt(receivedDate.toLocaleString());
    });

    return () => source.close();
  }, []);

  const routeRows = useMemo(
    () => Object.values(routes).sort((a, b) => b.eventCount - a.eventCount),
    [routes]
  );

  const propagationRows = useMemo(
    () => [...routeRows].sort((a, b) => b.averageDelay - a.averageDelay).slice(0, 8),
    [routeRows]
  );

  const strongestRoute = routeRows.length
    ? routeRows.reduce((best, route) => (route.reliabilityScore > best.reliabilityScore ? route : best), routeRows[0])
    : null;

  const weakestRoute = routeRows.length
    ? routeRows.reduce((worst, route) => (route.reliabilityScore < worst.reliabilityScore ? route : worst), routeRows[0])
    : null;

  const averageReliability = routeRows.length
    ? routeRows.reduce((sum, route) => sum + route.reliabilityScore, 0) / routeRows.length
    : 0;

  const averageDelay = routeRows.length
    ? routeRows.reduce((sum, route) => sum + route.averageDelay, 0) / routeRows.length
    : 0;

  const maxDelay = propagationRows.length ? Math.max(...propagationRows.map((row) => row.averageDelay), 1) : 1;

  return (
    <main className="app">
      <header className="hero">
        <div>
          <p className="eyebrow">AeroStream Command Center</p>
          <h1>Real-Time Flight Operations Dashboard</h1>
          <p className="hero-subtitle">
            Streaming route telemetry via server-sent events for live reliability and delay risk monitoring.
          </p>
        </div>
        <div className="hero-meta">
          <p className={`connection-pill ${status}`}>SSE {status}</p>
          <p className="updated-at">Last update: {lastUpdatedAt ?? "Awaiting live events"}</p>
        </div>
      </header>

      <section className="cards">
        <article>
          <h2>Routes Tracked</h2>
          <p>{routeRows.length}</p>
          <span>Active route snapshots</span>
        </article>
        <article>
          <h2>Avg Reliability</h2>
          <p>{averageReliability.toFixed(1)}</p>
          <span>Network confidence score</span>
        </article>
        <article>
          <h2>Avg Delay</h2>
          <p>{averageDelay.toFixed(1)}m</p>
          <span>Cross-route propagation baseline</span>
        </article>
      </section>

      <section className="dashboard-grid">
        <article className="panel live-table-panel">
          <div className="panel-header">
            <h2>Live Flight Table</h2>
            <span>{liveFlights.length} recent events</span>
          </div>
          <div className="table-shell">
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Flight</th>
                  <th>Route</th>
                  <th>Delay</th>
                  <th>Status</th>
                  <th>Reliability</th>
                </tr>
              </thead>
              <tbody>
                {liveFlights.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="empty-state">
                      Waiting for `route-update` SSE events from the analytics stream.
                    </td>
                  </tr>
                ) : (
                  liveFlights.map((flight) => (
                    <tr key={flight.id}>
                      <td>{flight.receivedAt}</td>
                      <td>{flight.flightCode}</td>
                      <td>
                        {flight.origin} -> {flight.destination}
                      </td>
                      <td>{flight.delayMinutes.toFixed(1)}m</td>
                      <td>
                        <span className={`status-chip ${flight.status.replace(" ", "-").toLowerCase()}`}>
                          {flight.status}
                        </span>
                      </td>
                      <td>{flight.reliabilityScore.toFixed(1)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </article>

        <article className="panel propagation-panel">
          <div className="panel-header">
            <h2>Delay Propagation Visualization</h2>
            <span>Top delay pressure routes</span>
          </div>
          <div className="bars">
            {propagationRows.length === 0 ? (
              <p className="empty-state">No route metrics yet.</p>
            ) : (
              propagationRows.map((route) => {
                const barWidth = Math.max((route.averageDelay / maxDelay) * 100, 4);
                return (
                  <div key={route.route} className="bar-row">
                    <div className="bar-labels">
                      <strong>{route.route}</strong>
                      <span>{route.averageDelay.toFixed(1)}m avg delay</span>
                    </div>
                    <div className="bar-track">
                      <div className="bar-fill" style={{ width: `${barWidth}%` }} />
                    </div>
                    <small>{route.eventCount} events</small>
                  </div>
                );
              })
            )}
          </div>
        </article>

        <article className="panel metrics-panel">
          <div className="panel-header">
            <h2>Route Reliability Metrics</h2>
            <span>Operational quality overview</span>
          </div>
          <div className="metric-grid">
            <div>
              <p>Best Route</p>
              <strong>{strongestRoute?.route ?? "N/A"}</strong>
              <span>{strongestRoute ? `${strongestRoute.reliabilityScore.toFixed(1)} score` : "No data"}</span>
            </div>
            <div>
              <p>Most Fragile Route</p>
              <strong>{weakestRoute?.route ?? "N/A"}</strong>
              <span>{weakestRoute ? `${weakestRoute.reliabilityScore.toFixed(1)} score` : "No data"}</span>
            </div>
          </div>
          <ul className="rank-list">
            {routeRows.slice(0, 6).map((route) => (
              <li key={route.route}>
                <span>{route.route}</span>
                <span>{reliabilityBadge(route.reliabilityScore)}</span>
                <strong>{route.reliabilityScore.toFixed(1)}</strong>
              </li>
            ))}
          </ul>
        </article>
      </section>
    </main>
  );
}
