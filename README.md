# ChargeFlow V2 — EV vs ICE Route Intelligence Engine

<div align="center">

[![Java](https://img.shields.io/badge/Java-11%2B-007396?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Architecture](https://img.shields.io/badge/Architecture-Factory%20%2B%20Strategy%20%2B%20Service-1f6feb?style=flat-square)](#-architecture)
[![Routing](https://img.shields.io/badge/Routing-OpenRouteService-2ea44f?style=flat-square)](https://openrouteservice.org/)
[![Charging%20Data](https://img.shields.io/badge/Charging%20Data-OpenChargeMap-f59e0b?style=flat-square)](https://openchargemap.org/site/develop)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

**A Java console platform that compares EV and ICE trips across any route, combining live routing + charging station discovery with deterministic cost, emissions, charging-stop simulation, and a weighted motivation score.**

[How It Works](#-how-it-works) · [Features](#-features) · [Architecture](#-architecture) · [Getting Started](#-getting-started) · [Scoring](#-motivation-score-model) · [Project Structure](#-project-structure)

</div>

---

## Project Overview

ChargeFlow V2 is a deterministic trip analysis engine designed for practical EV adoption decisions. For a given source and destination, it evaluates an EV and an ICE vehicle side-by-side on five dimensions:

- Route distance
- Charging feasibility (stops and station placement)
- Trip cost
- Carbon emissions
- Motivation score (0-100)

The engine is built with clear separation of concerns:

- **Factory pattern** for vehicle construction
- **Strategy pattern** for EV vs ICE cost computation
- **Service-layer orchestration** for route lookup, station discovery, battery simulation, and recommendations

It supports two execution modes:

- **Online mode (API-enabled):** live route geometry and charging stations from OpenRouteService + OpenChargeMap
- **Offline fallback mode:** static route database plus generated station estimates when APIs are unavailable

---

## How It Works

### Analysis Pipeline

```text
Input: source city + destination city + EV/ICE specs
    |
    v
Step 1  Vehicle Setup
        - Default pair (Tata Nexon EV vs Maruti Suzuki Brezza), or
        - Custom EV + custom ICE entered by user
    |
    v
Step 2  Route Resolution (RouteService)
        - Try OpenRouteService geocoding + directions
        - If API fails/not configured: RouteEngine static route DB fallback
    |
    v
Step 3  Charging Station Discovery (ChargingStationService)
        - If route coordinates available + keys configured:
          sample route waypoints and query OpenChargeMap
        - Deduplicate, validate names/coordinates, map to route km
        - Ensure route coverage and cap output size
        - Else: generate corridor-based fallback stations
    |
    v
Step 4  Battery Simulation (BatterySimulator)
        - Simulate travel with safety buffer and charge-to-80% policy
        - Compute total stops + total kWh consumed
    |
    v
Step 5  Cost Calculation (Strategy pattern)
        - EV cost via kWh consumption and electricity price
        - ICE cost via fuel consumption and fuel-type pricing
    |
    v
Step 6  Carbon Impact
        - EV and ICE emissions in kg CO2
        - Net CO2 saved by EV
    |
    v
Step 7  Recommendation + Motivation Score
        - Weighted recommendation: cost + emissions + convenience
        - Motivation score (0-100) with star label
    |
    v
Step 8  Console Report Rendering
        - Full route, station list, EV/ICE analysis, recommendation
```

### Deterministic by Design

No AI model and no probabilistic scoring is used. Given the same inputs and API responses, the outputs are reproducible.

---

## Features

**Live or Offline Routing**
- Uses OpenRouteService for geocoding and distance.
- Falls back to built-in route database if APIs fail.

**Charging Station Intelligence**
- Fetches stations along route from OpenChargeMap.
- Filters invalid/generic station names.
- Deduplicates by normalized identity + route position.
- Adds fallback stations to avoid route coverage gaps.

**Battery Stop Simulation**
- Uses EV range and kWh/km consumption.
- Applies a 30 km safety buffer.
- Charges to 80% whenever next waypoint is risky.

**Cost Strategy Engine**
- EV and ICE costs are computed via interchangeable strategy classes.
- Supports fuel-type-aware pricing for petrol and diesel.

**Carbon Impact Model**
- EV and ICE per-km emission factors.
- Clear CO2 saved value for each trip.

**Weighted Recommendation**
- Recommendation blends cost, emissions, and convenience.
- Returns both a label and human-readable reason.

**Motivation Score (0-100)**
- Weighted from 5 normalized components.
- Produces score label tiers from low to exceptional.

**Interactive CLI UX**
- Custom vehicle input support.
- Route validation and robust input handling.
- Readable summary with station-by-station visibility.

---

## Core Formulas

### EV Trip Cost

```text
energyConsumedKWh = distanceKm * evConsumptionPerKm
evCost = energyConsumedKWh * electricityPricePerKWh
```

### ICE Trip Cost

```text
fuelConsumedLitres = distanceKm / mileageKmPerLitre
iceCost = fuelConsumedLitres * fuelPricePerLitre
```

### Emissions

```text
evEmissionsKg  = distanceKm * 50 g/km / 1000
iceEmissionsKg = distanceKm * fuelEmissionFactor g/km / 1000
co2SavedKg     = iceEmissionsKg - evEmissionsKg
```

### Recommendation Score

```text
final = (costScore * 0.40)
      + (emissionScore * 0.35)
      + (convenienceScore * 0.25)

if final > 50 => EV Recommended else ICE Recommended
```

---

## Motivation Score Model

ChargeFlow V2 calculates a separate motivation score from five normalized components:

| Component | Weight | Basis |
|---|:---:|---|
| Cost Savings | 0.35 | EV vs ICE percentage savings |
| CO2 Reduction | 0.25 | Emission reduction percentage |
| Convenience | 0.20 | Charging stops per 100 km |
| Station Coverage | 0.10 | Stations per 100 km |
| Range Confidence | 0.10 | EV range to route distance ratio |

Final score:

```text
score = round(clamp0to100(weighted_sum_of_all_components))
```

Score labels:

- 90-100: `5 Star  Exceptional`
- 70-89: `4 Star  Strong`
- 50-69: `3 Star  Moderate`
- 30-49: `2 Star  Weak`
- 0-29: `1 Star  Low`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Runtime | JDK 11+ (uses `java.net.http.HttpClient`) |
| Networking | Built-in Java HTTP client |
| Routing API | OpenRouteService |
| Charging API | OpenChargeMap |
| Data Parsing | Custom lightweight `JsonParser` utility |
| Architecture | Factory, Strategy, Service, Builder patterns |
| Build | Direct `javac` compile (no Maven/Gradle required) |

---

## Project Structure

```text
ChargeFlow V2/
├── compile.bat
├── LICENSE
├── output.txt
├── sources.txt
├── strip_comments.js
└── src/
    └── com/
        └── chargeflow/
            ├── factory/
            │   └── VehicleFactory.java
            ├── main/
            │   └── ChargeFlowApp.java
            ├── model/
            │   ├── ChargingStation.java
            │   ├── EVVehicle.java
            │   ├── ICEVehicle.java
            │   ├── Route.java
            │   ├── TripSummary.java
            │   └── Vehicle.java
            ├── service/
            │   ├── BatterySimulator.java
            │   ├── CarbonCalculator.java
            │   ├── ChargingStationService.java
            │   ├── CostCalculator.java
            │   ├── MotivationScoreCalculator.java
            │   ├── RecommendationEngine.java
            │   ├── RouteEngine.java
            │   ├── RouteService.java
            │   ├── StationService.java
            │   └── TripAnalyzer.java
            ├── strategy/
            │   ├── CostStrategy.java
            │   ├── EVCostStrategy.java
            │   └── ICECostStrategy.java
            └── utils/
                ├── ApiConfig.java
                ├── DisplayUtils.java
                └── JsonParser.java
```

---

## Getting Started

### Prerequisites

- Java JDK 11 or newer
- Windows PowerShell or Command Prompt (for `compile.bat`)
- Optional API keys for full online mode:
  - OpenRouteService API key
  - OpenChargeMap API key

### 1) Clone

```bash
git clone https://github.com/parteek1907/ChargeFlow.git
cd "ChargeFlow V2"
```

### 2) Configure API keys (optional but recommended)

Edit `src/com/chargeflow/utils/ApiConfig.java`:

- `ORS_API_KEY`
- `OCM_API_KEY`

If keys are missing/invalid, the app still works with offline route + station fallback for supported corridors.

### 3) Compile and Run

On Windows:

```bat
compile.bat
```

Manual compile and run:

```bash
javac -d out src/com/chargeflow/model/*.java src/com/chargeflow/factory/*.java src/com/chargeflow/strategy/*.java src/com/chargeflow/service/*.java src/com/chargeflow/utils/*.java src/com/chargeflow/main/*.java
java -cp out com.chargeflow.main.ChargeFlowApp
```

---

## Example CLI Flow

```text
[SETUP] Choose comparison mode:
1. Default vehicles
2. Enter custom EV and ICE specifications

[INPUT] Enter source city:
[INPUT] Enter destination city:

...analysis output...

RECOMMENDATION:
EV Recommended
Reason: 83% lower cost, 58% fewer emissions

MOTIVATION SCORE:
73/100 (4 Star  Strong)
```

---

## API Integration Notes

### OpenRouteService

Used for:
- City geocoding (`/geocode/search`)
- Route directions and total distance (`/v2/directions/driving-car`)
- Reverse geocoding for fallback station city labels

### OpenChargeMap

Used for:
- Discovering nearby charging stations near sampled route coordinates
- Extracting station metadata (name, location, connector power)

### Offline Fallback Guarantees

If external APIs fail or timeout:

- Route falls back to static city-pair distances (`RouteEngine`)
- Charging stations fall back to deterministic corridor stations
- Full trip scoring and recommendation still complete

---

## Engineering Notes

**Station deduplication and cleanup**
- Invalid names (`public`, `unknown`, `test`, etc.) are filtered.
- Stations too close in route position are merged to reduce noise.

**Route sampling strategy**
- Route points are sampled with adaptive step sizing.
- Query count scales with route length and is capped to limit API pressure.

**Battery safety-first policy**
- A fixed safety reserve (30 km) avoids edge-case overrun.
- Charge events target 80% battery for practical fast-charge behavior.

**Extensible architecture**
- Additional vehicle types or cost policies can be integrated via new strategy implementations.

---

## Known Limitations

- Pricing constants (electricity and fuel) are static in code.
- Emission factors are fixed baseline values.
- JSON parser is lightweight and purpose-built for current APIs.
- Offline route DB includes selected Indian corridors, not full map coverage.

---

## Roadmap Ideas

- Dynamic fuel/electricity tariff integration
- Traffic-aware energy modeling
- Charger availability and queuing estimates
- Map UI frontend on top of this core engine
- Maven/Gradle packaging and unit test suite

---

## Author

**Parteek Garg**

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
