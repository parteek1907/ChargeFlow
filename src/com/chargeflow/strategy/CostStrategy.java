package com.chargeflow.strategy;

import com.chargeflow.model.Vehicle;

public interface CostStrategy {

    double calculateCost(double distanceKm, Vehicle vehicle);
}
