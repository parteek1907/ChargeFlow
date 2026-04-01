package com.chargeflow.service;

import com.chargeflow.model.*;
import com.chargeflow.strategy.*;
import java.util.List;

/**
 * TripAnalyzer — Orchestrator that ties all services together.
 * Upgraded: Now uses RouteService (API) and ChargingStationService (API)
 * instead of the old hardcoded RouteEngine and StationService.
 * 
 * All calculation logic (cost, carbon, recommendation, motivation, battery)
 * remains UNCHANGED — only the data sources are upgraded.
 */
public class TripAnalyzer {

    private final RouteService routeService;                 // NEW: API-based routes
    private final ChargingStationService stationService;     // NEW: API-based stations
    private final BatterySimulator batterySimulator;          // UNCHANGED
    private final CostCalculator costCalculator;             // UNCHANGED
    private final CarbonCalculator carbonCalculator;         // UNCHANGED
    private final RecommendationEngine recommendationEngine; // UNCHANGED
    private final MotivationScoreCalculator motivationScoreCalculator; // UNCHANGED

    public TripAnalyzer() {
        this.routeService = new RouteService();
        this.stationService = new ChargingStationService();
        this.batterySimulator = new BatterySimulator();
        this.costCalculator = new CostCalculator();
        this.carbonCalculator = new CarbonCalculator();
        this.recommendationEngine = new RecommendationEngine();
        this.motivationScoreCalculator = new MotivationScoreCalculator();
    }

    /**
     * Performs a complete trip analysis comparing EV vs ICE.
     * 
     * The ONLY change here is:
     *   - routeEngine.findRoute() → routeService.findRoute()
     *   - stationService.getStationsOnRoute() → stationService.findStationsAlongRoute()
     * 
     * Everything else (cost, carbon, recommendation, motivation, battery) is identical.
     */
    public TripSummary analyze(String source, String destination, EVVehicle ev, ICEVehicle ice) {

        // Step 1: Find route (NOW USES API)
        Route route = routeService.findRoute(source, destination);
        double distance = route.getDistanceKm();

        // Step 2: Find charging stations (NOW USES API)
        List<ChargingStation> stations = stationService.findStationsAlongRoute(route);

        // Step 3: Battery simulation (UNCHANGED)
        BatterySimulator.SimulationResult batteryResult = batterySimulator.simulate(ev, distance, stations);

        // Step 4: Cost calculation via Strategy Pattern (UNCHANGED)
        CostStrategy evStrategy = new EVCostStrategy();
        CostStrategy iceStrategy = new ICECostStrategy();
        double evCost = costCalculator.calculate(distance, ev, evStrategy);
        double iceCost = costCalculator.calculate(distance, ice, iceStrategy);
        double fuelConsumed = costCalculator.calculateFuelConsumed(distance, ice.getMileageKmPerLitre());

        // Step 5: Carbon emissions (UNCHANGED)
        double evEmissions = carbonCalculator.calculateEVEmissions(distance);
        double iceEmissions = carbonCalculator.calculateICEEmissions(distance, ice.getFuelType());
        double co2Saved = carbonCalculator.calculateSavings(iceEmissions, evEmissions);

        // Step 6: Recommendation (UNCHANGED)
        String recommendationLabel = recommendationEngine.getRecommendationLabel(
            evCost, iceCost, evEmissions, iceEmissions, batteryResult.getTotalStops()
        );
        String recommendationReason = recommendationEngine.getRecommendationReason(
            evCost, iceCost, evEmissions, iceEmissions, batteryResult.getTotalStops()
        );

        // Step 7: Motivation score (UNCHANGED)
        int motivationScore = motivationScoreCalculator.calculateScore(
            evCost, iceCost, evEmissions, iceEmissions
        );

        return new TripSummary.Builder()
            .route(route)
            .evName(ev.getName())
            .iceName(ice.getName())
            .evCost(evCost)
            .iceCost(iceCost)
            .evEmissionsKg(evEmissions)
            .iceEmissionsKg(iceEmissions)
            .co2SavedKg(co2Saved)
            .chargingStops(batteryResult.getTotalStops())
            .energyConsumedKWh(batteryResult.getTotalEnergyConsumedKWh())
            .fuelConsumedLitres(fuelConsumed)
            .recommendation(recommendationLabel)
            .recommendationReason(recommendationReason)
            .motivationScore(motivationScore)
            .stationsUsed(batteryResult.getStationsUsed())
            .allStationsOnRoute(stations)
            .build();
    }
}
