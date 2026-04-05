package com.chargeflow.model;

import java.util.ArrayList;
import java.util.List;

public class Route {

    private final String source;
    private final String destination;
    private final double distanceKm;
    private final List<double[]> routeCoordinates;

    public Route(String source, String destination, double distanceKm) {
        this.source = source;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.routeCoordinates = new ArrayList<>();
    }

    public Route(String source, String destination, double distanceKm,
                 List<double[]> routeCoordinates) {
        this.source = source;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.routeCoordinates = routeCoordinates != null ? routeCoordinates : new ArrayList<>();
    }

    public String getSource()               { return source; }
    public String getDestination()          { return destination; }
    public double getDistanceKm()           { return distanceKm; }
    public List<double[]> getRouteCoordinates() { return routeCoordinates; }

    public boolean hasCoordinates() {
        return routeCoordinates != null && !routeCoordinates.isEmpty();
    }

    public double[] getSourceCoordinates() {
        if (hasCoordinates()) return routeCoordinates.get(0);
        return null;
    }

    @Override
    public String toString() {
        return source + " -> " + destination + " (" + String.format("%.0f", distanceKm) + " km)";
    }
}
