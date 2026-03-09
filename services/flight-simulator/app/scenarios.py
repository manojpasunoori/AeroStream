from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
import random
from typing import Dict, List


AIRPORTS = ["DFW", "ORD", "JFK", "ATL", "LAX", "DEN", "PHX", "MIA"]
CARRIERS = ["AA", "DL", "UA", "WN", "B6"]


@dataclass
class ScenarioConfig:
    name: str
    delay_range: tuple[int, int]
    impact_airports: List[str]
    cascade_factor: float


SCENARIOS: Dict[str, ScenarioConfig] = {
    "clear": ScenarioConfig("clear", (0, 10), AIRPORTS, 0.05),
    "storm": ScenarioConfig("storm", (25, 120), ["DFW", "ATL", "ORD"], 0.35),
    "congestion": ScenarioConfig("congestion", (10, 65), ["JFK", "LAX", "DEN"], 0.2),
    "cascading": ScenarioConfig("cascading", (20, 90), AIRPORTS, 0.5),
}


def build_flight_event(scenario_name: str) -> dict:
    scenario = SCENARIOS.get(scenario_name, SCENARIOS["clear"])
    origin = random.choice(AIRPORTS)
    destination = random.choice([x for x in AIRPORTS if x != origin])
    impacted = origin in scenario.impact_airports or destination in scenario.impact_airports

    base_delay = random.randint(*scenario.delay_range)
    if impacted and random.random() < scenario.cascade_factor:
        base_delay += random.randint(10, 45)

    event_type = "ON_TIME" if base_delay <= 10 else "DELAYED"
    storm_flag = scenario_name == "storm"
    congestion_flag = scenario_name == "congestion"

    return {
        "event_version": "1.0.0",
        "event_id": f"evt-{random.randint(100000, 999999)}",
        "event_time": datetime.now(timezone.utc).isoformat(),
        "flight_id": f"{random.choice(CARRIERS)}-{random.randint(100, 9999)}",
        "carrier": random.choice(CARRIERS),
        "origin": origin,
        "destination": destination,
        "delay_minutes": base_delay,
        "event_type": event_type,
        "metadata": {
            "scenario": scenario_name,
            "storm": str(storm_flag).lower(),
            "congestion": str(congestion_flag).lower(),
            "cascading_likelihood": str(scenario.cascade_factor),
        },
    }

