package com.chargeflow.service;

import com.chargeflow.model.*;
import com.chargeflow.strategy.*;
import java.util.List;

public class TripAnalyzer {

    private final RouteService routeService;
    private final ChargingStationService stationService;
    private final BatterySimulator batterySimulator;
    private final CostCalculator costCalculator;
    private final CarbonCalculator carbonCalculator;
    private final RecommendationEngine recommendationEngine;
    private final MotivationScoreCalculator motivationScoreCalculator;

    public TripAnalyzer() {
        this.routeService = new RouteService();
        this.stationService = new ChargingStationService();
        this.batterySimulator = new BatterySimulator();
        this.costCalculator = new CostCalculator();
        this.carbonCalculator = new CarbonCalculator();
        this.recommendationEngine = new RecommendationEngine();
        this.motivationScoreCalculator = new MotivationScoreCalculator();
    }

    public TripSummary analyze(String source, String destination, EVVehicle ev, ICEVehicle ice) {

        Route route = routeService.findRoute(source, destination);
        double distance = route.getDistanceKm();

        List<ChargingStation> stations = stationService.findStationsAlongRoute(route);

        BatterySimulator.SimulationResult batteryResult = batterySimulator.simulate(ev, distance, stations);

        CostStrategy evStrategy = new EVCostStrategy();
        CostStrategy iceStrategy = new ICECostStrategy();
        double evCost = costCalculator.calculate(distance, ev, evStrategy);
        double iceCost = costCalculator.calculate(distance, ice, iceStrategy);
        double fuelConsumed = costCalculator.calculateFuelConsumed(distance, ice.getMileageKmPerLitre());

        double evEmissions = carbonCalculator.calculateEVEmissions(distance);
        double iceEmissions = carbonCalculator.calculateICEEmissions(distance, ice.getFuelType());
        double co2Saved = carbonCalculator.calculateSavings(iceEmissions, evEmissions);

        String recommendationLabel = recommendationEngine.getRecommendationLabel(
            evCost, iceCost, evEmissions, iceEmissions, batteryResult.getTotalStops()
        );
        String recommendationReason = recommendationEngine.getRecommendationReason(
            evCost, iceCost, evEmissions, iceEmissions, batteryResult.getTotalStops()
        );

        int motivationScore = motivationScoreCalculator.calculateScore(
            evCost,
            iceCost,
            evEmissions,
            iceEmissions,
            distance,
            batteryResult.getTotalStops(),
            stations.size(),
            ev.getRangeKm()
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
