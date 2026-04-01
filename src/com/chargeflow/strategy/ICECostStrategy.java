package com.chargeflow.strategy;

import com.chargeflow.model.Vehicle;
import com.chargeflow.model.ICEVehicle;

public class ICECostStrategy implements CostStrategy {

    private static final double PETROL_PRICE_PER_LITRE = 105.0;
    private static final double DIESEL_PRICE_PER_LITRE = 92.0;

    @Override
    public double calculateCost(double distanceKm, Vehicle vehicle) {

        ICEVehicle ice = (ICEVehicle) vehicle;

        double fuelConsumedLitres = distanceKm / ice.getMileageKmPerLitre();

        double fuelPrice = getFuelPrice(ice.getFuelType());

        double cost = fuelConsumedLitres * fuelPrice;

        return cost;
    }

    private double getFuelPrice(String fuelType) {
        switch (fuelType.toLowerCase()) {
            case "diesel":
                return DIESEL_PRICE_PER_LITRE;
            case "petrol":
            default:
                return PETROL_PRICE_PER_LITRE;
        }
    }

    public double getPetrolPrice() {
        return PETROL_PRICE_PER_LITRE;
    }

    public double getDieselPrice() {
        return DIESEL_PRICE_PER_LITRE;
    }
}
