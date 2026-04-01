package com.chargeflow.service;

import com.chargeflow.model.Vehicle;
import com.chargeflow.strategy.CostStrategy;

public class CostCalculator {

    public double calculate(double distanceKm, Vehicle vehicle, CostStrategy strategy) {
        return strategy.calculateCost(distanceKm, vehicle);
    }

    public double calculateFuelConsumed(double distanceKm, double mileageKmPerL) {
        return distanceKm / mileageKmPerL;
    }
}
