package com.chargeflow.service;

import com.chargeflow.model.ChargingStation;
import com.chargeflow.model.EVVehicle;
import java.util.*;

public class BatterySimulator {

    private static final double CHARGE_TARGET_PERCENT = 0.80;
    private static final double SAFETY_BUFFER_KM = 30.0;

    public SimulationResult simulate(EVVehicle ev, double totalDistanceKm, List<ChargingStation> availableStations) {

        double batteryKWh = ev.getBatteryCapacityKWh();
        double remainingRangeKm = ev.getRangeKm();
        double consumptionPerKm = ev.getEnergyConsumptionPerKm();
        double currentPositionKm = 0;
        double totalEnergyConsumed = 0;

        List<ChargingStation> stopsUsed = new ArrayList<>();

        if (remainingRangeKm >= totalDistanceKm + SAFETY_BUFFER_KM) {
            totalEnergyConsumed = totalDistanceKm * consumptionPerKm;
            return new SimulationResult(stopsUsed, 0, totalEnergyConsumed, totalDistanceKm);
        }

        for (int i = 0; i < availableStations.size(); i++) {
            ChargingStation station = availableStations.get(i);
            double distanceToStation = station.getLocationKm() - currentPositionKm;

            if (distanceToStation <= 0) continue;

            if (remainingRangeKm - SAFETY_BUFFER_KM < distanceToStation) {
                continue;
            }

            double energyUsed = distanceToStation * consumptionPerKm;
            remainingRangeKm -= distanceToStation;
            batteryKWh -= energyUsed;
            totalEnergyConsumed += energyUsed;
            currentPositionKm = station.getLocationKm();

            double nextWaypointKm;

            if (i + 1 < availableStations.size()) {
                nextWaypointKm = availableStations.get(i + 1).getLocationKm();
            } else {
                nextWaypointKm = totalDistanceKm;
            }

            double distanceToNextWaypoint = nextWaypointKm - currentPositionKm;

            if (remainingRangeKm - SAFETY_BUFFER_KM < distanceToNextWaypoint) {
                stopsUsed.add(station);

                double chargeTargetKWh = ev.getBatteryCapacityKWh() * CHARGE_TARGET_PERCENT;
                batteryKWh = chargeTargetKWh;
                remainingRangeKm = batteryKWh / consumptionPerKm;
            }
        }

        double finalLegDistance = totalDistanceKm - currentPositionKm;
        if (finalLegDistance > 0) {
            double finalLegEnergy = finalLegDistance * consumptionPerKm;
            totalEnergyConsumed += finalLegEnergy;
        }

        return new SimulationResult(
            stopsUsed,
            stopsUsed.size(),
            totalEnergyConsumed,
            totalDistanceKm
        );
    }

    public static class SimulationResult {

        private final List<ChargingStation> stationsUsed;
        private final int totalStops;
        private final double totalEnergyConsumedKWh;
        private final double totalDistanceKm;

        public SimulationResult(List<ChargingStation> stationsUsed, int totalStops,
                                 double totalEnergyConsumedKWh, double totalDistanceKm) {
            this.stationsUsed = stationsUsed;
            this.totalStops = totalStops;
            this.totalEnergyConsumedKWh = totalEnergyConsumedKWh;
            this.totalDistanceKm = totalDistanceKm;
        }

        public List<ChargingStation> getStationsUsed() { return stationsUsed; }
        public int getTotalStops()                     { return totalStops; }
        public double getTotalEnergyConsumedKWh()      { return totalEnergyConsumedKWh; }
        public double getTotalDistanceKm()             { return totalDistanceKm; }
    }
}
