package com.chargeflow.model;

public class ICEVehicle extends Vehicle {

    private final String fuelType;
    private final double mileageKmPerLitre;
    private final double tankCapacityLitres;

    public ICEVehicle(String name, String fuelType, double mileageKmPerLitre, double tankCapacityLitres) {
        super(name, "ICE");
        this.fuelType = fuelType;
        this.mileageKmPerLitre = mileageKmPerLitre;
        this.tankCapacityLitres = tankCapacityLitres;
    }

    public String getFuelType() {
        return fuelType;
    }

    public double getMileageKmPerLitre() {
        return mileageKmPerLitre;
    }

    public double getTankCapacityLitres() {
        return tankCapacityLitres;
    }

    @Override
    public double getEfficiency() {
        return mileageKmPerLitre;
    }

    @Override
    public String describe() {
        return String.format(
            "%s [%s] | Mileage: %.1f km/L | Tank: %.0f L | Range: %.0f km",
            getName(), fuelType, mileageKmPerLitre, tankCapacityLitres,
            mileageKmPerLitre * tankCapacityLitres
        );
    }
}
