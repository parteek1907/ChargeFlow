package com.chargeflow.service;

import com.chargeflow.model.Route;
import java.util.*;

public class RouteEngine {

    private static final Map<String, Double> ROUTE_DATABASE = new HashMap<>();

    static {
        ROUTE_DATABASE.put("DELHI-JAIPUR", 281.0);
        ROUTE_DATABASE.put("DELHI-AGRA", 233.0);
        ROUTE_DATABASE.put("MUMBAI-PUNE", 149.0);
        ROUTE_DATABASE.put("BANGALORE-MYSORE", 143.0);
        ROUTE_DATABASE.put("CHENNAI-PONDICHERRY", 170.0);
        ROUTE_DATABASE.put("DELHI-CHANDIGARH", 248.0);
        ROUTE_DATABASE.put("HYDERABAD-VIJAYAWADA", 272.0);
        ROUTE_DATABASE.put("AHMEDABAD-VADODARA", 112.0);
        ROUTE_DATABASE.put("DELHI-MUMBAI", 1424.0);
    }

    public Route findRoute(String source, String destination) {
        String normalizedSource = source.trim();
        String normalizedDest = destination.trim();
        String key = normalizedSource.toUpperCase() + "-" + normalizedDest.toUpperCase();

        Double distance = ROUTE_DATABASE.get(key);

        if (distance == null) {
            String reverseKey = normalizedDest.toUpperCase() + "-" + normalizedSource.toUpperCase();
            distance = ROUTE_DATABASE.get(reverseKey);
        }

        if (distance == null) {
            throw new IllegalArgumentException(
                "Route not found: " + normalizedSource + " -> " + normalizedDest + "\n" +
                "Available routes: " + getAvailableRoutes()
            );
        }

        return new Route(normalizedSource, normalizedDest, distance);
    }

    public String getAvailableRoutes() {
        StringBuilder sb = new StringBuilder();
        for (String key : ROUTE_DATABASE.keySet()) {
            sb.append(key.replace("-", " -> ")).append(", ");
        }
        return sb.toString();
    }
}

