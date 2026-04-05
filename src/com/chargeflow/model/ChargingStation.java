package com.chargeflow.model;

public class ChargingStation {

    private final String name;
    private final String city;
    private final String state;
    private final String country;
    private final double locationKm;
    private final String chargerType;
    private final double pricePerUnit;
    private final double latitude;
    private final double longitude;

    public ChargingStation(String name, double locationKm, String chargerType, double pricePerUnit) {
        this.name = name;
        this.city = "Unknown";
        this.state = "";
        this.country = "";
        this.locationKm = locationKm;
        this.chargerType = chargerType;
        this.pricePerUnit = pricePerUnit;
        this.latitude = 0;
        this.longitude = 0;
    }

    public ChargingStation(String name, String city, String state, String country, double locationKm, String chargerType,
                           double pricePerUnit, double latitude, double longitude) {
        this.name = name;
        this.city = city;
        this.state = state;
        this.country = country;
        this.locationKm = locationKm;
        this.chargerType = chargerType;
        this.pricePerUnit = pricePerUnit;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
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
