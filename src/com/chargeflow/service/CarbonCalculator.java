package com.chargeflow.service;

public class CarbonCalculator {

    private static final double EV_EMISSIONS_GRAMS_PER_KM     = 50.0;   
    private static final double PETROL_EMISSIONS_GRAMS_PER_KM = 120.0;  
    private static final double DIESEL_EMISSIONS_GRAMS_PER_KM = 110.0;  

    public double calculateEVEmissions(double distanceKm) {
        double gramsTotal = distanceKm * EV_EMISSIONS_GRAMS_PER_KM;
        return gramsTotal / 1000.0;  
    }

    public double calculateICEEmissions(double distanceKm, String fuelType) {
        double gramsPerKm = getEmissionFactor(fuelType);
        double gramsTotal = distanceKm * gramsPerKm;
        return gramsTotal / 1000.0;  
    }

    public double calculateSavings(double iceEmissionsKg, double evEmissionsKg) {
        return iceEmissionsKg - evEmissionsKg;
    }

    private double getEmissionFactor(String fuelType) {
        if ("Diesel".equalsIgnoreCase(fuelType)) {
            return DIESEL_EMISSIONS_GRAMS_PER_KM;
        }
        return PETROL_EMISSIONS_GRAMS_PER_KM;  
    }
}
