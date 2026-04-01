package com.chargeflow.model;

import java.util.List;

public class TripSummary {

    private final Route route;
    private final String evName;
    private final String iceName;
    private final double evCost;
    private final double iceCost;
    private final double evEmissionsKg;
    private final double iceEmissionsKg;
    private final double co2SavedKg;
    private final int chargingStops;
    private final double energyConsumedKWh;
    private final double fuelConsumedLitres;
    private final String recommendation;
    private final String recommendationReason;
    private final int motivationScore;
    private final List<ChargingStation> stationsUsed;
    private final List<ChargingStation> allStationsOnRoute;

    private TripSummary(Builder builder) {
        this.route = builder.route;
        this.evName = builder.evName;
        this.iceName = builder.iceName;
        this.evCost = builder.evCost;
        this.iceCost = builder.iceCost;
        this.evEmissionsKg = builder.evEmissionsKg;
        this.iceEmissionsKg = builder.iceEmissionsKg;
        this.co2SavedKg = builder.co2SavedKg;
        this.chargingStops = builder.chargingStops;
        this.energyConsumedKWh = builder.energyConsumedKWh;
        this.fuelConsumedLitres = builder.fuelConsumedLitres;
        this.recommendation = builder.recommendation;
        this.recommendationReason = builder.recommendationReason;
        this.motivationScore = builder.motivationScore;
        this.stationsUsed = builder.stationsUsed;
        this.allStationsOnRoute = builder.allStationsOnRoute;
    }

    public Route getRoute()                   { return route; }
    public String getEvName()                 { return evName; }
    public String getIceName()                { return iceName; }
    public double getEvCost()                 { return evCost; }
    public double getIceCost()                { return iceCost; }
    public double getEvEmissionsKg()          { return evEmissionsKg; }
    public double getIceEmissionsKg()         { return iceEmissionsKg; }
    public double getCo2SavedKg()             { return co2SavedKg; }
    public int getChargingStops()             { return chargingStops; }
    public double getEnergyConsumedKWh()      { return energyConsumedKWh; }
    public double getFuelConsumedLitres()     { return fuelConsumedLitres; }
    public String getRecommendation()         { return recommendation; }
    public String getRecommendationReason()   { return recommendationReason; }
    public int getMotivationScore()           { return motivationScore; }
    public List<ChargingStation> getStationsUsed() { return stationsUsed; }
    public List<ChargingStation> getAllStationsOnRoute() { return allStationsOnRoute; }

    public static class Builder {

        private Route route;
        private String evName;
        private String iceName;
        private double evCost;
        private double iceCost;
        private double evEmissionsKg;
        private double iceEmissionsKg;
        private double co2SavedKg;
        private int chargingStops;
        private double energyConsumedKWh;
        private double fuelConsumedLitres;
        private String recommendation;
        private String recommendationReason;
        private int motivationScore;
        private List<ChargingStation> stationsUsed;
        private List<ChargingStation> allStationsOnRoute;

        public Builder route(Route route)                           { this.route = route; return this; }
        public Builder evName(String evName)                        { this.evName = evName; return this; }
        public Builder iceName(String iceName)                      { this.iceName = iceName; return this; }
        public Builder evCost(double evCost)                        { this.evCost = evCost; return this; }
        public Builder iceCost(double iceCost)                      { this.iceCost = iceCost; return this; }
        public Builder evEmissionsKg(double evEmissionsKg)          { this.evEmissionsKg = evEmissionsKg; return this; }
        public Builder iceEmissionsKg(double iceEmissionsKg)        { this.iceEmissionsKg = iceEmissionsKg; return this; }
        public Builder co2SavedKg(double co2SavedKg)                { this.co2SavedKg = co2SavedKg; return this; }
        public Builder chargingStops(int chargingStops)             { this.chargingStops = chargingStops; return this; }
        public Builder energyConsumedKWh(double energyConsumedKWh)  { this.energyConsumedKWh = energyConsumedKWh; return this; }
        public Builder fuelConsumedLitres(double fuelConsumedLitres) { this.fuelConsumedLitres = fuelConsumedLitres; return this; }
        public Builder recommendation(String recommendation)        { this.recommendation = recommendation; return this; }
        public Builder recommendationReason(String reason)           { this.recommendationReason = reason; return this; }
        public Builder motivationScore(int motivationScore)          { this.motivationScore = motivationScore; return this; }
        public Builder stationsUsed(List<ChargingStation> stations)  { this.stationsUsed = stations; return this; }
        public Builder allStationsOnRoute(List<ChargingStation> stations) { this.allStationsOnRoute = stations; return this; }

        public TripSummary build() {
            return new TripSummary(this);
        }
    }
}
