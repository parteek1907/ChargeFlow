package com.chargeflow.service;

public class MotivationScoreCalculator {

    private static final double COST_SAVINGS_WEIGHT = 0.35;
    private static final double CO2_REDUCTION_WEIGHT = 0.25;
    private static final double CONVENIENCE_WEIGHT = 0.20;
    private static final double STATION_COVERAGE_WEIGHT = 0.10;
    private static final double RANGE_CONFIDENCE_WEIGHT = 0.10;

    public int calculateScore(double evCost, double iceCost,
                               double evEmissionsKg, double iceEmissionsKg,
                               double routeDistanceKm, int chargingStops,
                               int stationsOnRoute, double evRangeKm) {

        double costSavingsPercent = 0;
        if (iceCost > 0) {
            costSavingsPercent = ((iceCost - evCost) / iceCost) * 100;
        }

        double co2ReductionPercent = 0;
        if (iceEmissionsKg > 0) {
            co2ReductionPercent = ((iceEmissionsKg - evEmissionsKg) / iceEmissionsKg) * 100;
        }

        double routeDistance = Math.max(1.0, routeDistanceKm);

        double stopsPer100Km = chargingStops * 100.0 / routeDistance;
        double convenienceScore = 100.0 - (stopsPer100Km * 35.0);

        double stationsPer100Km = stationsOnRoute * 100.0 / routeDistance;
        double stationCoverageScore = stationsPer100Km * 30.0;

        double rangeRatio = evRangeKm / routeDistance;
        double rangeConfidenceScore = rangeRatio * 100.0;

        double normalizedCostScore = clamp(costSavingsPercent);
        double normalizedCo2Score = clamp(co2ReductionPercent);
        double normalizedConvenienceScore = clamp(convenienceScore);
        double normalizedStationCoverageScore = clamp(stationCoverageScore);
        double normalizedRangeConfidenceScore = clamp(rangeConfidenceScore);

        double rawScore = (normalizedCostScore * COST_SAVINGS_WEIGHT)
                        + (normalizedCo2Score * CO2_REDUCTION_WEIGHT)
                        + (normalizedConvenienceScore * CONVENIENCE_WEIGHT)
                        + (normalizedStationCoverageScore * STATION_COVERAGE_WEIGHT)
                        + (normalizedRangeConfidenceScore * RANGE_CONFIDENCE_WEIGHT);

        int finalScore = (int) Math.round(clamp(rawScore));

        return finalScore;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    public String getScoreLabel(int score) {
        if (score >= 90) return "5 Star  Exceptional";
        if (score >= 70) return "4 Star  Strong";
        if (score >= 50) return "3 Star  Moderate";
        if (score >= 30) return "2 Star  Weak";
        return                  "1 Star  Low";
    }
}
