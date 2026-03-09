import { useEffect, useMemo, useState } from "react";

type RouteSnapshot = {
  route: string;
  eventCount: number;
  averageDelay: number;
  reliabilityScore: number;
};

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export function App() {
  const [routes, setRoutes] = useState<Record<string, RouteSnapshot>>({});
  const [status, setStatus] = useState("connecting");

  useEffect(() => {
    const source = new EventSource(`${API_BASE}/api/analytics/stream`);

    source.onopen = () => setStatus("connected");
    source.onerror = () => setStatus("reconnecting");
    source.addEventListener("route-update", (evt: MessageEvent) => {
      const payload = JSON.parse(evt.data) as RouteSnapshot;
      setRoutes((prev) => ({ ...prev, [payload.route]: payload }));
    });

    return () => source.close();
  }, []);

  const rows = useMemo(
    () => Object.values(routes).sort((a, b) => b.eventCount - a.eventCount),
    [routes]
  );

  return (
    <main className="app">
      <header>
        <h1>AeroStream Live Operations</h1>
        <p className={`status ${status}`}>SSE: {status}</p>
      </header>

      <section className="cards">
        <article>
          <h2>Routes Tracked</h2>
          <p>{rows.length}</p>
        </article>
        <article>
          <h2>Avg Reliability</h2>
          <p>
            {rows.length
              ? (rows.reduce((sum, r) => sum + r.reliabilityScore, 0) / rows.length).toFixed(1)
              : "0.0"}
          </p>
        </article>
      </section>

      <section>
        <table>
          <thead>
            <tr>
              <th>Route</th>
              <th>Events</th>
              <th>Avg Delay (min)</th>
              <th>Reliability</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.route}>
                <td>{r.route}</td>
                <td>{r.eventCount}</td>
                <td>{r.averageDelay.toFixed(1)}</td>
                <td>{r.reliabilityScore.toFixed(1)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  );
}
