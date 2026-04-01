package com.chargeflow.strategy;

import com.chargeflow.model.Vehicle;
import com.chargeflow.model.EVVehicle;

public class EVCostStrategy implements CostStrategy {

    private static final double ELECTRICITY_PRICE_PER_KWH = 8.0;

    @Override
    public double calculateCost(double distanceKm, Vehicle vehicle) {

        EVVehicle ev = (EVVehicle) vehicle;

        double energyConsumedKWh = distanceKm * ev.getEnergyConsumptionPerKm();

        double cost = energyConsumedKWh * ELECTRICITY_PRICE_PER_KWH;

        return cost;
    }

    public double getElectricityPrice() {
        return ELECTRICITY_PRICE_PER_KWH;
    }
}
