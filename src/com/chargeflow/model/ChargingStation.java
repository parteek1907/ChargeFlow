package com.chargeflow.model;

/**
 * ChargingStation — Represents an EV charging station.
 * Upgraded: Now supports latitude/longitude for real API-sourced stations.
 */
public class ChargingStation {

    private final String name;
    private final double locationKm;      // Distance from route start in km
    private final String chargerType;
    private final double pricePerUnit;    // Price per kWh in ₹
    private final double latitude;
    private final double longitude;

    // Original constructor (backward compatible — for hardcoded/fallback stations)
    public ChargingStation(String name, double locationKm, String chargerType, double pricePerUnit) {
        this.name = name;
        this.locationKm = locationKm;
        this.chargerType = chargerType;
        this.pricePerUnit = pricePerUnit;
        this.latitude = 0;
        this.longitude = 0;
    }

    // New constructor with coordinates (for API-sourced stations)
    public ChargingStation(String name, double locationKm, String chargerType,
                           double pricePerUnit, double latitude, double longitude) {
        this.name = name;
        this.locationKm = locationKm;
        this.chargerType = chargerType;
        this.pricePerUnit = pricePerUnit;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public double getLocationKm() {
        return locationKm;
    }

    public String getChargerType() {
        return chargerType;
    }

    public double getPricePerUnit() {
        return pricePerUnit;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return String.format(
            "%s @ %.0f km [%s] — ₹%.1f/kWh",
            name, locationKm, chargerType, pricePerUnit
        );
    }
}
