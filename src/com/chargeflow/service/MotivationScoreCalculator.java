package com.chargeflow.service;

public class MotivationScoreCalculator {

    private static final double COST_SAVINGS_WEIGHT = 0.60;
    private static final double CO2_REDUCTION_WEIGHT = 0.40;

    public int calculateScore(double evCost, double iceCost,
                               double evEmissionsKg, double iceEmissionsKg) {

        double costSavingsPercent = 0;
        if (iceCost > 0) {
            costSavingsPercent = ((iceCost - evCost) / iceCost) * 100;
        }

        double co2ReductionPercent = 0;
        if (iceEmissionsKg > 0) {
            co2ReductionPercent = ((iceEmissionsKg - evEmissionsKg) / iceEmissionsKg) * 100;
        }

        double rawScore = (costSavingsPercent * COST_SAVINGS_WEIGHT)
                        + (co2ReductionPercent * CO2_REDUCTION_WEIGHT);

        int finalScore = (int) Math.round(Math.max(0, Math.min(100, rawScore)));

        return finalScore;
    }

    public String getScoreLabel(int score) {
        if (score >= 90) return "5 Star  Exceptional";
        if (score >= 70) return "4 Star  Strong";
        if (score >= 50) return "3 Star  Moderate";
        if (score >= 30) return "2 Star  Weak";
        return                  "1 Star  Low";
    }
}
