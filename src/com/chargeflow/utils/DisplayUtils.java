package com.chargeflow.utils;

import com.chargeflow.model.ChargingStation;
import com.chargeflow.model.TripSummary;
import com.chargeflow.service.MotivationScoreCalculator;
import java.util.List;

public class DisplayUtils {

    public static void printTripSummary(TripSummary summary) {
        System.out.println();
        System.out.println("=== CHARGEFLOW TRIP ANALYSIS ===");
        System.out.println();

        System.out.println("ROUTE:");
        System.out.println(summary.getRoute().getSource() + " -> " + summary.getRoute().getDestination()
            + " (" + String.format("%.0f", summary.getRoute().getDistanceKm()) + " km)");
        System.out.println();

        System.out.println("CHARGING STATIONS:");
        List<ChargingStation> allStations = summary.getAllStationsOnRoute();
        if (allStations != null && !allStations.isEmpty()) {
            int count = 1;
            for (ChargingStation station : allStations) {
                String location = formatLocation(station.getCity(), station.getState(), station.getCountry());

                System.out.printf("  %d. %s -- %s -- %.0f km%n", count, station.getName(), location, station.getLocationKm());
                count++;
            }
        } else {
            System.out.println("  No charging stations found along route.");
        }
        System.out.println();

        System.out.println("EV ANALYSIS:");
        System.out.println("  Vehicle:          " + summary.getEvName());
        System.out.printf("  Energy consumed:  %.2f kWh%n", summary.getEnergyConsumedKWh());
        System.out.printf("  Charging stops:   %d%n", summary.getChargingStops());
        if (summary.getStationsUsed() != null && !summary.getStationsUsed().isEmpty()) {
            System.out.println("  Stops at:");
            for (ChargingStation station : summary.getStationsUsed()) {
                System.out.printf("    - %s (%.0f km) - Rs. %.1f/kWh%n",
                    station.getName(), station.getLocationKm(), station.getPricePerUnit());
            }
        }
        System.out.printf("  Trip cost:        Rs. %.2f%n", summary.getEvCost());
        System.out.println();

        System.out.println("ICE ANALYSIS:");
        System.out.println("  Vehicle:          " + summary.getIceName());
        System.out.printf("  Fuel consumed:    %.2f L%n", summary.getFuelConsumedLitres());
        System.out.printf("  Trip cost:        Rs. %.2f%n", summary.getIceCost());
        System.out.println();

        System.out.println("ENVIRONMENTAL IMPACT:");
        System.out.printf("  EV emissions:     %.2f kg CO2%n", summary.getEvEmissionsKg());
        System.out.printf("  ICE emissions:    %.2f kg CO2%n", summary.getIceEmissionsKg());
        System.out.printf("  CO2 saved:        %.2f kg%n", summary.getCo2SavedKg());
        System.out.println();

        System.out.println("RECOMMENDATION:");
        System.out.println("  " + summary.getRecommendation().replace("⚡ ", "").replace("⛽ ", ""));
        System.out.println("  Reason: " + summary.getRecommendationReason());
        System.out.println();

        System.out.println("MOTIVATION SCORE:");
        int score = summary.getMotivationScore();
        System.out.printf("  %d/100 (%s)%n", score, new MotivationScoreCalculator().getScoreLabel(score));
        System.out.println("  " + renderScoreBar(score));
        System.out.println();
        System.out.println("================================");
        System.out.println();
    }

    public static void printBanner() {
        System.out.println();
        System.out.println("=======================================================");
        System.out.println("                 CHARGEFLOW V2                         ");
        System.out.println("       EV Route Analysis & Comparison Engine           ");
        System.out.println("=======================================================");
        System.out.println();
    }

    public static void printVehicleInfo(String evDescription, String iceDescription) {
        System.out.println("Vehicles Being Compared:");
        System.out.println("-------------------------------------------------------");
        System.out.println("  [EV]  " + evDescription.replace("⚡ ", ""));
        System.out.println("  [ICE] " + iceDescription.replace("⛽ ", ""));
        System.out.println();
    }

    private static String renderScoreBar(int score) {
        int filled = score / 5;
        int empty = 20 - filled;

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < filled; i++) bar.append("|");
        for (int i = 0; i < empty; i++)  bar.append(" ");
        bar.append("]");

        return bar.toString();
    }

    private static String formatLocation(String city, String state, String country) {
        String safeCity = sanitizeLocationPart(city);
        String safeState = sanitizeLocationPart(state);
        String safeCountry = sanitizeLocationPart(country);

        StringBuilder sb = new StringBuilder();
        if (!safeCity.isEmpty()) {
            sb.append(safeCity);
        }
        if (!safeState.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(safeState);
        }
        if (!safeCountry.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(safeCountry);
        }

        return sb.length() > 0 ? sb.toString() : "Along route (estimated)";
    }

    private static String sanitizeLocationPart(String value) {
        if (value == null) return "";
        String cleaned = value.trim();
        if (cleaned.isEmpty()) return "";

        String normalized = cleaned.toLowerCase();
        if (normalized.equals("unknown") || normalized.equals("null") || normalized.equals("n/a") || normalized.equals("na")) {
            return "";
        }

        return cleaned;
    }
}

