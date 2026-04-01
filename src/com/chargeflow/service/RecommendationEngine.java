package com.chargeflow.service;

public class RecommendationEngine {

    private static final double COST_WEIGHT       = 0.40;
    private static final double EMISSION_WEIGHT   = 0.35;
    private static final double CONVENIENCE_WEIGHT = 0.25;

    private static final int CONVENIENCE_THRESHOLD_STOPS = 3;

    public String recommend(double evCost, double iceCost,
                            double evEmissionsKg, double iceEmissionsKg,
                            int chargingStops) {

        double costScore = calculateCostScore(evCost, iceCost);

        double emissionScore = calculateEmissionScore(evEmissionsKg, iceEmissionsKg);

        double convenienceScore = calculateConvenienceScore(chargingStops);

        double finalScore = (costScore * COST_WEIGHT)
                          + (emissionScore * EMISSION_WEIGHT)
                          + (convenienceScore * CONVENIENCE_WEIGHT);

        String recommendation;
        String reason;

        if (finalScore > 50) {
            recommendation = "⚡ EV Recommended";

            double costSavingsPercent = ((iceCost - evCost) / iceCost) * 100;
            double emissionSavingsPercent = ((iceEmissionsKg - evEmissionsKg) / iceEmissionsKg) * 100;

            reason = String.format("%.0f%% lower cost, %.0f%% fewer emissions",
                    costSavingsPercent, emissionSavingsPercent);

            if (chargingStops == 0) {
                reason += ", no charging stops needed";
            }
        } else {
            recommendation = "⛽ ICE Recommended";
            reason = "Better suited for this route";

            if (chargingStops > CONVENIENCE_THRESHOLD_STOPS) {
                reason += String.format(" (%d charging stops make EV inconvenient)", chargingStops);
            }
            if (evCost > iceCost) {
                reason += ", EV is more expensive for this trip";
            }
        }

        return recommendation + "\n   Reason: " + reason;
    }

    public String getRecommendationLabel(double evCost, double iceCost,
                                          double evEmissionsKg, double iceEmissionsKg,
                                          int chargingStops) {
        double costScore = calculateCostScore(evCost, iceCost);
        double emissionScore = calculateEmissionScore(evEmissionsKg, iceEmissionsKg);
        double convenienceScore = calculateConvenienceScore(chargingStops);
        double finalScore = (costScore * COST_WEIGHT)
                          + (emissionScore * EMISSION_WEIGHT)
                          + (convenienceScore * CONVENIENCE_WEIGHT);

        return finalScore > 50 ? "⚡ EV Recommended" : "⛽ ICE Recommended";
    }

    public String getRecommendationReason(double evCost, double iceCost,
                                           double evEmissionsKg, double iceEmissionsKg,
                                           int chargingStops) {
        double costScore = calculateCostScore(evCost, iceCost);
        double emissionScore = calculateEmissionScore(evEmissionsKg, iceEmissionsKg);
        double convenienceScore = calculateConvenienceScore(chargingStops);
        double finalScore = (costScore * COST_WEIGHT)
                          + (emissionScore * EMISSION_WEIGHT)
                          + (convenienceScore * CONVENIENCE_WEIGHT);

        if (finalScore > 50) {
            double costSavingsPercent = ((iceCost - evCost) / iceCost) * 100;
            double emissionSavingsPercent = ((iceEmissionsKg - evEmissionsKg) / iceEmissionsKg) * 100;
            String reason = String.format("%.0f%% lower cost, %.0f%% fewer emissions",
                    costSavingsPercent, emissionSavingsPercent);
            if (chargingStops == 0) reason += ", no charging stops needed";
            return reason;
        } else {
            String reason = "Better suited for this route";
            if (chargingStops > CONVENIENCE_THRESHOLD_STOPS) {
                reason += String.format(" (%d charging stops make EV inconvenient)", chargingStops);
            }
            return reason;
        }
    }

    private double calculateCostScore(double evCost, double iceCost) {
        if (iceCost == 0) return 50;  
        double savingsPercent = ((iceCost - evCost) / iceCost) * 100;
        return Math.max(0, Math.min(100, savingsPercent));
    }

    private double calculateEmissionScore(double evEmissionsKg, double iceEmissionsKg) {
        if (iceEmissionsKg == 0) return 50;
        double reductionPercent = ((iceEmissionsKg - evEmissionsKg) / iceEmissionsKg) * 100;
        return Math.max(0, Math.min(100, reductionPercent));
    }

    private double calculateConvenienceScore(int chargingStops) {
        if (chargingStops == 0) return 100;
        if (chargingStops <= CONVENIENCE_THRESHOLD_STOPS) {

            return 100 - (chargingStops * 20);
        }

        return Math.max(0, 40 - ((chargingStops - CONVENIENCE_THRESHOLD_STOPS) * 15));
    }
}
