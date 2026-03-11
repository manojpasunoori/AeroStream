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

type TimePoint = { t: string; avgDelay: number; avgReliability: number; eventRate: number };
type ConnectionState = "connecting" | "connected" | "reconnecting";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8086";
const SIMULATOR_BASE = "http://localhost:8091";
const LIVE_TABLE_LIMIT = 40;
const HISTORY_LIMIT = 30;

const reliabilityBadge = (score: number) => {
  if (score >= 85) return "Excellent";
  if (score >= 70) return "Stable";
  return "At Risk";
};

const reliabilityColor = (score: number) => {
  if (score >= 85) return "#48bb78";
  if (score >= 70) return "#ecc94b";
  return "#fc8181";
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

// SVG sparkline from array of 0–100 normalized values
function Sparkline({ values, color }: { values: number[]; color: string }) {
  if (values.length < 2) return <svg width="80" height="24" />;
  const max = Math.max(...values, 1);
  const min = Math.min(...values);
  const range = max - min || 1;
  const W = 80; const H = 24;
  const pts = values.map((v, i) => {
    const x = (i / (values.length - 1)) * W;
    const y = H - ((v - min) / range) * (H - 4) - 2;
    return `${x},${y}`;
  }).join(" ");
  return (
    <svg width={W} height={H} style={{ display: "block" }}>
      <polyline points={pts} fill="none" stroke={color} strokeWidth="1.5" strokeLinejoin="round" />
    </svg>
  );
}

// SVG line chart for time-series data
function LineChart({ history, field, color, label }: {
  history: TimePoint[];
  field: "avgDelay" | "avgReliability";
  color: string;
  label: string;
}) {
  const W = 100; const H = 60;
  if (history.length < 2) {
    return (
      <div className="linechart-wrap">
        <p className="linechart-label">{label}</p>
        <svg width="100%" height={H} viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none">
          <text x="50" y="35" textAnchor="middle" fill="#4a5568" fontSize="6">Awaiting data...</text>
        </svg>
      </div>
    );
  }
  const values = history.map(h => h[field]);
  const max = Math.max(...values, 1);
  const min = Math.min(...values, 0);
  const range = max - min || 1;
  const pts = values.map((v, i) => {
    const x = (i / (values.length - 1)) * W;
    const y = H - ((v - min) / range) * (H - 8) - 4;
    return `${x},${y}`;
  }).join(" ");
  const latest = values[values.length - 1];
  return (
    <div className="linechart-wrap">
      <div className="linechart-header">
        <p className="linechart-label">{label}</p>
        <strong style={{ color }}>{field === "avgDelay" ? `${latest.toFixed(1)}m` : latest.toFixed(1)}</strong>
      </div>
      <svg width="100%" height={H} viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none">
        <defs>
          <linearGradient id={`grad-${field}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity="0.3" />
            <stop offset="100%" stopColor={color} stopOpacity="0" />
          </linearGradient>
        </defs>
        {/* fill area */}
        <polygon
          points={`0,${H} ${pts} ${W},${H}`}
          fill={`url(#grad-${field})`}
        />
        <polyline points={pts} fill="none" stroke={color} strokeWidth="1.5" strokeLinejoin="round" />
        {/* latest dot */}
        {(() => {
          const last = pts.split(" ").pop()!;
          const [lx, ly] = last.split(",");
          return <circle cx={lx} cy={ly} r="2.5" fill={color} />;
        })()}
      </svg>
      <div className="linechart-axis">
        <span>{history[0]?.t}</span>
        <span>{history[history.length - 1]?.t}</span>
      </div>
    </div>
  );
}

export function App() {
  const [routes, setRoutes] = useState<Record<string, RouteSnapshot>>({});
  const [liveFlights, setLiveFlights] = useState<FlightEventRow[]>([]);
  const [status, setStatus] = useState<ConnectionState>("connecting");
  const [lastUpdatedAt, setLastUpdatedAt] = useState<string | null>(null);
  const [history, setHistory] = useState<TimePoint[]>([]);
  const [routeHistory, setRouteHistory] = useState<Record<string, number[]>>({});
  const [scenario, setScenario] = useState<"normal" | "storm">("normal");
  const [scenarioLoading, setScenarioLoading] = useState(false);
  const [totalEvents, setTotalEvents] = useState(0);
  const [eventRate, setEventRate] = useState(0);
  const sequenceRef = useRef(0);
  const eventCountRef = useRef(0);
  const lastRateRef = useRef(Date.now());

  // Load initial snapshot
  useEffect(() => {
    let mounted = true;
    const load = async () => {
      try {
        const res = await fetch(`${API_BASE}/api/analytics/routes/reliability`);
        if (!res.ok) return;
        const payload = (await res.json()) as Record<string, Omit<RouteSnapshot, "route">>;
        if (!mounted) return;
        const normalized = Object.entries(payload).reduce<Record<string, RouteSnapshot>>(
          (acc, [route, metrics]) => {
            acc[route] = { route, eventCount: metrics.eventCount ?? 0, averageDelay: metrics.averageDelay ?? 0, reliabilityScore: metrics.reliabilityScore ?? 100 };
            return acc;
          }, {}
        );
        setRoutes(normalized);
      } catch { /* dashboard still works via SSE */ }
    };
    void load();
    return () => { mounted = false; };
  }, []);

  // SSE stream
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

      // Track event rate
      eventCountRef.current += 1;
      const now = Date.now();
      const elapsed = (now - lastRateRef.current) / 1000;
      if (elapsed >= 5) {
        setEventRate(Math.round(eventCountRef.current / elapsed));
        eventCountRef.current = 0;
        lastRateRef.current = now;
      }

      setTotalEvents(prev => prev + 1);
      setRoutes(prev => ({ ...prev, [payload.route]: payload }));

      // Per-route reliability sparkline history
      setRouteHistory(prev => {
        const existing = prev[payload.route] ?? [];
        return { ...prev, [payload.route]: [...existing, payload.reliabilityScore].slice(-20) };
      });

      setLiveFlights(prev => {
        const event: FlightEventRow = {
          id: `${payload.route}-${currentSequence}`,
          flightCode: `FL-${currentSequence.toString().padStart(4, "0")}`,
          route: payload.route, origin, destination,
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

  // Build time-series history from route snapshots every 5s
  useEffect(() => {
    const interval = setInterval(() => {
      setRoutes(current => {
        const rows = Object.values(current);
        if (rows.length === 0) return current;
        const avgDelay = rows.reduce((s, r) => s + r.averageDelay, 0) / rows.length;
        const avgReliability = rows.reduce((s, r) => s + r.reliabilityScore, 0) / rows.length;
        const t = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
        setHistory(prev => [...prev, { t, avgDelay, avgReliability, eventRate }].slice(-HISTORY_LIMIT));
        return current;
      });
    }, 5000);
    return () => clearInterval(interval);
  }, [eventRate]);

  const toggleScenario = async () => {
    setScenarioLoading(true);
    const next = scenario === "normal" ? "storm" : "normal";
    try {
      await fetch(`${SIMULATOR_BASE}/simulation/start`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ scenario: next }),
      });
      setScenario(next);
    } catch {
      alert("Could not reach simulator at " + SIMULATOR_BASE);
    } finally {
      setScenarioLoading(false);
    }
  };

  const routeRows = useMemo(() => Object.values(routes).sort((a, b) => b.eventCount - a.eventCount), [routes]);
  const propagationRows = useMemo(() => [...routeRows].sort((a, b) => b.averageDelay - a.averageDelay).slice(0, 8), [routeRows]);
  const strongestRoute = routeRows.length ? routeRows.reduce((b, r) => r.reliabilityScore > b.reliabilityScore ? r : b, routeRows[0]) : null;
  const weakestRoute = routeRows.length ? routeRows.reduce((w, r) => r.reliabilityScore < w.reliabilityScore ? r : w, routeRows[0]) : null;
  const averageReliability = routeRows.length ? routeRows.reduce((s, r) => s + r.reliabilityScore, 0) / routeRows.length : 0;
  const averageDelay = routeRows.length ? routeRows.reduce((s, r) => s + r.averageDelay, 0) / routeRows.length : 0;
  const maxDelay = propagationRows.length ? Math.max(...propagationRows.map(r => r.averageDelay), 1) : 1;

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
          <button
            className={`scenario-btn ${scenario === "storm" ? "storm-active" : ""}`}
            onClick={toggleScenario}
            disabled={scenarioLoading}
          >
            {scenarioLoading ? "Switching..." : scenario === "storm" ? "🌩 Storm Mode — Click to Clear" : "☀️ Normal Mode — Click for Storm"}
          </button>
        </div>
      </header>

      {/* KPI Cards */}
      <section className="cards">
        <article>
          <h2>Routes Tracked</h2>
          <p>{routeRows.length}</p>
          <span>Active route snapshots</span>
        </article>
        <article>
          <h2>Avg Reliability</h2>
          <p style={{ color: reliabilityColor(averageReliability) }}>{averageReliability.toFixed(1)}</p>
          <span>Network confidence score</span>
        </article>
        <article>
          <h2>Avg Delay</h2>
          <p>{averageDelay.toFixed(1)}m</p>
          <span>Cross-route propagation baseline</span>
        </article>
        <article>
          <h2>Total Events</h2>
          <p>{totalEvents.toLocaleString()}</p>
          <span>{eventRate} events/sec</span>
        </article>
      </section>

      {/* Time-Series Charts */}
      <section className="charts-row">
        <article className="panel">
          <div className="panel-header">
            <h2>Network Delay Trend</h2>
            <span>Last {history.length} samples</span>
          </div>
          <LineChart history={history} field="avgDelay" color="#63b3ed" label="Avg Delay (min)" />
        </article>
        <article className="panel">
          <div className="panel-header">
            <h2>Network Reliability Trend</h2>
            <span>Last {history.length} samples</span>
          </div>
          <LineChart history={history} field="avgReliability" color="#48bb78" label="Avg Reliability Score" />
        </article>
      </section>

      <section className="dashboard-grid">
        {/* Live Table */}
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
                  <tr><td colSpan={6} className="empty-state">Waiting for route-update SSE events from the analytics stream.</td></tr>
                ) : (
                  liveFlights.map(flight => (
                    <tr key={flight.id}>
                      <td>{flight.receivedAt}</td>
                      <td>{flight.flightCode}</td>
                      <td>{flight.origin} → {flight.destination}</td>
                      <td>{flight.delayMinutes.toFixed(1)}m</td>
                      <td>
                        <span className={`status-chip ${flight.status.replace(/ /g, "-").toLowerCase()}`}>
                          {flight.status}
                        </span>
                      </td>
                      <td style={{ color: reliabilityColor(flight.reliabilityScore) }}>
                        {flight.reliabilityScore.toFixed(1)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </article>

        {/* Delay Propagation Bars */}
        <article className="panel propagation-panel">
          <div className="panel-header">
            <h2>Delay Propagation</h2>
            <span>Top delay pressure routes</span>
          </div>
          <div className="bars">
            {propagationRows.length === 0 ? (
              <p className="empty-state">No route metrics yet.</p>
            ) : (
              propagationRows.map(route => {
                const barWidth = Math.max((route.averageDelay / maxDelay) * 100, 4);
                const delayColor = route.averageDelay > 30 ? "#fc8181" : route.averageDelay > 15 ? "#ecc94b" : "#48bb78";
                return (
                  <div key={route.route} className="bar-row">
                    <div className="bar-labels">
                      <strong>{route.route}</strong>
                      <span style={{ color: delayColor }}>{route.averageDelay.toFixed(1)}m avg delay</span>
                    </div>
                    <div className="bar-track">
                      <div className="bar-fill" style={{ width: `${barWidth}%`, background: `linear-gradient(90deg, ${delayColor}99, ${delayColor})` }} />
                    </div>
                    <small>{route.eventCount} events</small>
                  </div>
                );
              })
            )}
          </div>
        </article>

        {/* Route Reliability with Sparklines */}
        <article className="panel metrics-panel">
          <div className="panel-header">
            <h2>Route Reliability</h2>
            <span>Score + trend sparkline</span>
          </div>
          <div className="metric-grid">
            <div>
              <p>Best Route</p>
              <strong>{strongestRoute?.route ?? "N/A"}</strong>
              <span style={{ color: "#48bb78" }}>{strongestRoute ? `${strongestRoute.reliabilityScore.toFixed(1)} score` : "No data"}</span>
            </div>
            <div>
              <p>Most Fragile Route</p>
              <strong>{weakestRoute?.route ?? "N/A"}</strong>
              <span style={{ color: "#fc8181" }}>{weakestRoute ? `${weakestRoute.reliabilityScore.toFixed(1)} score` : "No data"}</span>
            </div>
          </div>
          <ul className="rank-list">
            {routeRows.slice(0, 6).map(route => (
              <li key={route.route}>
                <span>{route.route}</span>
                <Sparkline values={routeHistory[route.route] ?? []} color={reliabilityColor(route.reliabilityScore)} />
                <span className="badge" style={{ color: reliabilityColor(route.reliabilityScore) }}>
                  {reliabilityBadge(route.reliabilityScore)}
                </span>
                <strong style={{ color: reliabilityColor(route.reliabilityScore) }}>
                  {route.reliabilityScore.toFixed(1)}
                </strong>
              </li>
            ))}
          </ul>
        </article>
      </section>
    </main>
  );
}