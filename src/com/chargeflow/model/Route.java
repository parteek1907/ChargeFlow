package com.chargeflow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Route — Represents a travel path between two locations.
 * Upgraded: Now supports route coordinates for station proximity detection.
 */
public class Route {

    private final String source;
    private final String destination;
    private final double distanceKm;
    private final List<String> segments;
    private final List<double[]> routeCoordinates; // [lon, lat] pairs along route

    // Original constructor (backward compatible)
    public Route(String source, String destination, double distanceKm, List<String> segments) {
        this.source = source;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.segments = segments;
        this.routeCoordinates = new ArrayList<>();
    }

    // New constructor with coordinates (for API-based routes)
    public Route(String source, String destination, double distanceKm,
                 List<String> segments, List<double[]> routeCoordinates) {
        this.source = source;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.segments = segments;
        this.routeCoordinates = routeCoordinates != null ? routeCoordinates : new ArrayList<>();
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public List<String> getSegments() {
        return segments;
    }

    public List<double[]> getRouteCoordinates() {
        return routeCoordinates;
    }

    public boolean hasCoordinates() {
        return routeCoordinates != null && !routeCoordinates.isEmpty();
    }

    @Override
    public String toString() {
        return source + " → " + destination + " (" + String.format("%.1f", distanceKm) + " km)";
    }
}
