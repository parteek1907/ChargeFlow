package com.chargeflow.service;

import com.chargeflow.model.Route;
import com.chargeflow.utils.ApiConfig;
import com.chargeflow.utils.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * RouteService — Fetches real route data using OpenRouteService API.
 * 
 * Flow:
 *   1. Geocode source city → [lon, lat]
 *   2. Geocode destination city → [lon, lat]
 *   3. Get directions between the two → distance + route coordinates
 *   4. Build Route object with real data
 * 
 * Falls back to the old hardcoded RouteEngine if API fails.
 */
public class RouteService {

    private final HttpClient httpClient;
    private final RouteEngine fallbackEngine; // Old hardcoded routes as fallback

    public RouteService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
            .build();
        this.fallbackEngine = new RouteEngine();
    }

    /**
     * Finds a route between two cities.
     * Tries API first, falls back to hardcoded data on failure.
     */
    public Route findRoute(String source, String destination) {
        if (!ApiConfig.isConfigured()) {
            System.out.println("  [WARN] API keys not configured. Using offline route database.");
            return fallbackEngine.findRoute(source, destination);
        }

        try {
            System.out.println("  [INFO] Connecting to OpenRouteService API...");

            // Step 1: Geocode source city
            System.out.println("  [GEO] Geocoding: " + source + "...");
            double[] sourceCoords = geocode(source);
            if (sourceCoords == null) {
                throw new RuntimeException("Could not find city: " + source);
            }

            // Step 2: Geocode destination city
            System.out.println("  [GEO] Geocoding: " + destination + "...");
            double[] destCoords = geocode(destination);
            if (destCoords == null) {
                throw new RuntimeException("Could not find city: " + destination);
            }

            // Step 3: Get directions
            System.out.println("  [ROUTE] Fetching route: " + source + " -> " + destination + "...");
            return getDirections(source, destination, sourceCoords, destCoords);

        } catch (Exception e) {
            System.out.println("  [WARN] API error: " + e.getMessage());
            System.out.println("  [INFO] Falling back to offline route database...");
            try {
                return fallbackEngine.findRoute(source, destination);
            } catch (IllegalArgumentException fallbackError) {
                throw new IllegalArgumentException(
                    "Route not found for: " + source + " -> " + destination + "\n" +
                    "  API failed and no offline data available for this route.\n" +
                    "  Tip: Configure your API keys in ApiConfig.java for unlimited routes."
                );
            }
        }
    }

    /**
     * Geocodes a city name to [longitude, latitude] coordinates.
     * Uses OpenRouteService Geocode API.
     * 
     * API: GET /geocode/search?text=CityName&boundary.country=IN&size=1
     */
    private double[] geocode(String cityName) throws Exception {
        String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8);

        String url = String.format(
            "%s?api_key=%s&text=%s&boundary.country=%s&size=1",
            ApiConfig.ORS_GEOCODE_URL,
            ApiConfig.ORS_API_KEY,
            encodedCity,
            ApiConfig.COUNTRY_BIAS
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.out.println("  [WARN] Geocode API returned status: " + response.statusCode());
            return null;
        }

        String body = response.body();

        // Extract coordinates from the first feature
        String features = JsonParser.getArray(body, "features");
        if (features.isEmpty() || features.equals("[]")) {
            return null;
        }

        // Get first feature's geometry coordinates
        return JsonParser.extractCoordinates(body);
    }

    /**
     * Gets driving directions between two coordinate pairs.
     * Uses OpenRouteService Directions API.
     * 
     * API: GET /v2/directions/driving-car?start=lon,lat&end=lon,lat
     */
    private Route getDirections(String source, String destination,
                                 double[] sourceCoords, double[] destCoords) throws Exception {

        String url = String.format(
            "%s?api_key=%s&start=%f,%f&end=%f,%f",
            ApiConfig.ORS_DIRECTIONS_URL,
            ApiConfig.ORS_API_KEY,
            sourceCoords[0], sourceCoords[1],  // lon, lat
            destCoords[0], destCoords[1]
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Directions API returned status: " + response.statusCode());
        }

        String body = response.body();

        // Extract distance from summary
        String summary = JsonParser.getObject(body, "summary");
        double distanceMeters = JsonParser.getDouble(summary, "distance");

        if (distanceMeters <= 0) {
            throw new RuntimeException("Could not parse route distance from API response");
        }

        double distanceKm = distanceMeters / 1000.0;

        // Extract route coordinates from geometry
        List<double[]> routeCoordinates = extractRouteCoordinates(body);

        // Extract real step-by-step navigation segments
        List<String> segments = extractSegments(body);
        if (segments.isEmpty()) {
            segments.add(String.format("Segment 1: %s -> %s (%.1f km)", source, destination, distanceKm));
        }

        System.out.println("  [OK] Route found: " + String.format("%.1f", distanceKm) + " km");

        return new Route(source, destination, distanceKm, segments, routeCoordinates);
    }

    /**
     * Extracts route coordinate pairs from the ORS directions response geometry.
     * The coordinates are in the GeoJSON format: [[lon,lat], [lon,lat], ...]
     */
    private List<double[]> extractRouteCoordinates(String responseBody) {
        List<double[]> coordinates = new ArrayList<>();

        // Find the coordinates array in the geometry
        String coordsArray = JsonParser.getArray(responseBody, "coordinates");
        if (coordsArray.isEmpty()) return coordinates;

        // Parse the nested array: [[lon1,lat1],[lon2,lat2],...]
        List<String> coordPairs = JsonParser.splitArray(coordsArray);

        for (String pair : coordPairs) {
            // Each pair is like "[77.209, 28.613]"
            if (!pair.startsWith("[")) continue;

            String inner = pair.substring(1, pair.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length >= 2) {
                try {
                    double lon = Double.parseDouble(parts[0].trim());
                    double lat = Double.parseDouble(parts[1].trim());
                    coordinates.add(new double[]{lon, lat});
                } catch (NumberFormatException e) {
                    // Skip malformed coordinates
                }
            }
        }

        return coordinates;
    }

    /**
     * Extracts real human-readable route segments from the API's steps array.
     */
    private List<String> extractSegments(String responseBody) {
        List<String> segments = new ArrayList<>();

        String stepsArray = JsonParser.getArray(responseBody, "steps");
        if (stepsArray.isEmpty()) return segments;

        List<String> stepsList = JsonParser.splitArray(stepsArray);
        int segmentNumber = 1;

        for (String stepStr : stepsList) {
            String instruction = JsonParser.getString(stepStr, "instruction");
            double stepDistMeters = JsonParser.getDouble(stepStr, "distance");
            double stepDistKm = stepDistMeters / 1000.0;

            if (instruction.isEmpty()) {
                instruction = "Continue on route";
            }

            segments.add(String.format("Segment %d: %s (%.1f km)", segmentNumber, instruction, stepDistKm));
            segmentNumber++;
        }

        return segments;
    }
}
