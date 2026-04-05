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

public class RouteService {

    private final HttpClient httpClient;

    public RouteService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
            .build();
    }

    public Route findRoute(String source, String destination) {
        if (!ApiConfig.isConfigured()) {
            System.out.println("  [WARN] API keys not configured. Using offline route database.");
            return offlineFallback(source, destination);
        }

        try {
            System.out.println("  [INFO] Connecting to OpenRouteService API...");

            System.out.println("  [GEO] Geocoding: " + source + "...");
            double[] sourceCoords = geocode(source);
            if (sourceCoords == null) {
                throw new RuntimeException("Could not find city: " + source);
            }

            System.out.println("  [GEO] Geocoding: " + destination + "...");
            double[] destCoords = geocode(destination);
            if (destCoords == null) {
                throw new RuntimeException("Could not find city: " + destination);
            }

            System.out.println("  [ROUTE] Fetching route: " + source + " -> " + destination + "...");
            return getDirections(source, destination, sourceCoords, destCoords);

        } catch (Exception e) {
            System.out.println("  [WARN] API error: " + e.getMessage());
            System.out.println("  [INFO] Falling back to offline route database...");
            return offlineFallback(source, destination);
        }
    }

    private Route offlineFallback(String source, String destination) {
        try {
            RouteEngine engine = new RouteEngine();
            return engine.findRoute(source, destination);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Route not found for: " + source + " -> " + destination + "\n" +
                "  API failed and no offline data available for this route.\n" +
                "  Tip: Configure your API keys in ApiConfig.java for unlimited routes."
            );
        }
    }

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

        String features = JsonParser.getArray(body, "features");
        if (features.isEmpty() || features.equals("[]")) {
            return null;
        }

        return JsonParser.extractCoordinates(body);
    }

    private Route getDirections(String source, String destination,
                                 double[] sourceCoords, double[] destCoords) throws Exception {

        String url = String.format(
            "%s?api_key=%s&start=%f,%f&end=%f,%f",
            ApiConfig.ORS_DIRECTIONS_URL,
            ApiConfig.ORS_API_KEY,
            sourceCoords[0], sourceCoords[1],
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

        String summary = JsonParser.getObject(body, "summary");
        double distanceMeters = JsonParser.getDouble(summary, "distance");

        if (distanceMeters <= 0) {
            throw new RuntimeException("Could not parse route distance from API response");
        }

        double distanceKm = distanceMeters / 1000.0;

        List<double[]> routeCoordinates = extractRouteCoordinates(body);

        System.out.println("  [OK] Route found: " + String.format("%.0f", distanceKm) + " km (" + routeCoordinates.size() + " coordinate points)");

        return new Route(source, destination, distanceKm, routeCoordinates);
    }

    private List<double[]> extractRouteCoordinates(String responseBody) {
        List<double[]> coordinates = new ArrayList<>();

        String coordsArray = JsonParser.getArray(responseBody, "coordinates");
        if (coordsArray.isEmpty()) return coordinates;

        List<String> coordPairs = JsonParser.splitArray(coordsArray);

        for (String pair : coordPairs) {
            if (!pair.startsWith("[")) continue;

            String inner = pair.substring(1, pair.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length >= 2) {
                try {
                    double lon = Double.parseDouble(parts[0].trim());
                    double lat = Double.parseDouble(parts[1].trim());
                    coordinates.add(new double[]{lon, lat});
                } catch (NumberFormatException e) {
                }
            }
        }

        return coordinates;
    }
}
