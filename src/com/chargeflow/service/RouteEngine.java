package com.chargeflow.service;

import com.chargeflow.model.Route;
import java.util.*;

public class RouteEngine {

    private static class RouteData {
        double distanceKm;
        List<String> segments;

        RouteData(double distanceKm, List<String> segments) {
            this.distanceKm = distanceKm;
            this.segments = segments;
        }
    }

    private static final Map<String, RouteData> ROUTE_DATABASE = new HashMap<>();

    static {

        ROUTE_DATABASE.put("DELHI-JAIPUR", new RouteData(281, Arrays.asList(
            "Delhi → Manesar (35 km)",
            "Manesar → Dharuhera (25 km)",
            "Dharuhera → Neemrana (62 km)",
            "Neemrana → Behror (22 km)",
            "Behror → Shahpura (55 km)",
            "Shahpura → Jaipur (82 km)"
        )));

        ROUTE_DATABASE.put("DELHI-AGRA", new RouteData(233, Arrays.asList(
            "Delhi → Noida (20 km)",
            "Noida → Mathura (133 km)",
            "Mathura → Agra (80 km)"
        )));

        ROUTE_DATABASE.put("MUMBAI-PUNE", new RouteData(149, Arrays.asList(
            "Mumbai → Panvel (30 km)",
            "Panvel → Lonavala (65 km)",
            "Lonavala → Pune (54 km)"
        )));

        ROUTE_DATABASE.put("BANGALORE-MYSORE", new RouteData(143, Arrays.asList(
            "Bangalore → Ramanagara (50 km)",
            "Ramanagara → Mandya (47 km)",
            "Mandya → Mysore (46 km)"
        )));

        ROUTE_DATABASE.put("CHENNAI-PONDICHERRY", new RouteData(170, Arrays.asList(
            "Chennai → Mahabalipuram (58 km)",
            "Mahabalipuram → Tindivanam (52 km)",
            "Tindivanam → Pondicherry (60 km)"
        )));

        ROUTE_DATABASE.put("DELHI-CHANDIGARH", new RouteData(248, Arrays.asList(
            "Delhi → Panipat (90 km)",
            "Panipat → Karnal (35 km)",
            "Karnal → Ambala (65 km)",
            "Ambala → Chandigarh (58 km)"
        )));

        ROUTE_DATABASE.put("HYDERABAD-VIJAYAWADA", new RouteData(272, Arrays.asList(
            "Hyderabad → Nalgonda (92 km)",
            "Nalgonda → Suryapet (48 km)",
            "Suryapet → Khammam (72 km)",
            "Khammam → Vijayawada (60 km)"
        )));

        ROUTE_DATABASE.put("AHMEDABAD-VADODARA", new RouteData(112, Arrays.asList(
            "Ahmedabad → Nadiad (47 km)",
            "Nadiad → Anand (18 km)",
            "Anand → Vadodara (47 km)"
        )));

        ROUTE_DATABASE.put("DELHI-MUMBAI", new RouteData(1424, Arrays.asList(
            "Delhi → Jaipur (281 km)",
            "Jaipur → Udaipur (396 km)",
            "Udaipur → Ahmedabad (262 km)",
            "Ahmedabad → Vadodara (112 km)",
            "Vadodara → Surat (160 km)",
            "Surat → Mumbai (213 km)"
        )));
    }

    public Route findRoute(String source, String destination) {

        String normalizedSource = source.trim();
        String normalizedDest = destination.trim();
        String key = normalizedSource.toUpperCase() + "-" + normalizedDest.toUpperCase();

        RouteData data = ROUTE_DATABASE.get(key);

        if (data == null) {
            String reverseKey = normalizedDest.toUpperCase() + "-" + normalizedSource.toUpperCase();
            data = ROUTE_DATABASE.get(reverseKey);

            if (data != null) {

                List<String> reversedSegments = reverseSegments(data.segments, normalizedSource, normalizedDest);
                return new Route(normalizedSource, normalizedDest, data.distanceKm, reversedSegments);
            }
        }

        if (data == null) {
            throw new IllegalArgumentException(
                "Route not found: " + normalizedSource + " → " + normalizedDest + "\n" +
                "Available routes: " + getAvailableRoutes()
            );
        }

        return new Route(normalizedSource, normalizedDest, data.distanceKm, data.segments);
    }

    public String getAvailableRoutes() {
        StringBuilder sb = new StringBuilder();
        for (String key : ROUTE_DATABASE.keySet()) {
            sb.append(key.replace("-", " → ")).append(", ");
        }
        return sb.toString();
    }

    private List<String> reverseSegments(List<String> originalSegments, String source, String destination) {
        List<String> reversed = new ArrayList<>(originalSegments);
        Collections.reverse(reversed);
        return reversed;
    }
}
