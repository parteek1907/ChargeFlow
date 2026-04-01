package com.chargeflow.service;

import com.chargeflow.model.ChargingStation;
import com.chargeflow.model.Route;
import com.chargeflow.utils.ApiConfig;
import com.chargeflow.utils.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * ChargingStationService — Fetches real charging station data using OpenChargeMap API.
 * 
 * Flow:
 *   1. Sample waypoints along the route (every ~50 km)
 *   2. For each waypoint, query OpenChargeMap for nearby stations
 *   3. Filter duplicates (same station found from multiple waypoints)
 *   4. Assign locationKm based on cumulative distance
 *   5. Sort by distance from route start
 * 
 * Falls back to generating dummy stations if API fails.
 */
public class ChargingStationService {

    private final HttpClient httpClient;

    // Default charging price when OCM doesn't provide pricing (₹/kWh)
    private static final double DEFAULT_PRICE_PER_KWH = 14.0;

    public ChargingStationService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
            .build();
    }

    /**
     * Finds charging stations along a route.
     * Uses API if configured and route has coordinates, otherwise generates fallback data.
     */
    public List<ChargingStation> findStationsAlongRoute(Route route) {
        if (!ApiConfig.isConfigured() || !route.hasCoordinates()) {
            System.out.println("  [INFO] Using estimated charging station data.");
            return generateFallbackStations(route.getDistanceKm());
        }

        try {
            System.out.println("  [STATION] Searching for real charging stations along route...");
            List<ChargingStation> stations = fetchStationsFromAPI(route);

            if (stations.isEmpty()) {
                System.out.println("  [WARN] No stations found from API. Using estimated data.");
                return generateFallbackStations(route.getDistanceKm());
            }

            System.out.println("  [OK] Found " + stations.size() + " charging stations along route.");
            return stations;

        } catch (Exception e) {
            System.out.println("  [WARN] Station API error: " + e.getMessage());
            System.out.println("  [INFO] Using estimated charging station data.");
            return generateFallbackStations(route.getDistanceKm());
        }
    }

    /**
     * Queries OpenChargeMap API at sampled waypoints along the route.
     */
    private List<ChargingStation> fetchStationsFromAPI(Route route) throws Exception {
        List<double[]> coordinates = route.getRouteCoordinates();
        double totalDistanceKm = route.getDistanceKm();
        int totalCoords = coordinates.size();

        if (totalCoords == 0) return new ArrayList<>();

        // Ensure we sample roughly every 20-30 km, but avoid busting API limits
        // The user specifically wanted a step iteration. We will calculate an index step.
        // E.g. skip `stepSize` coordinate points to match the ~20km distance.
        int stepSize = Math.max(1, (int) (totalCoords * 25.0 / totalDistanceKm));

        // Track seen stations by name to avoid duplicates
        Set<String> seenStations = new HashSet<>();
        List<ChargingStation> allStations = new ArrayList<>();

        int queriesMade = 0;
        int maxQueries = 20; // Ensure we don't spam the free tier API

        for (int i = 0; i < totalCoords && queriesMade < maxQueries; i += stepSize) {
            double[] waypoint = coordinates.get(i);
            double lat = waypoint[1];
            double lon = waypoint[0];

            // Estimate km position of this waypoint
            double waypointKm = (double) i / totalCoords * totalDistanceKm;

            List<ChargingStation> nearby = queryOpenChargeMap(lat, lon, waypointKm);
            queriesMade++;

            for (ChargingStation station : nearby) {
                // Deduplicate by name
                String normalizedName = station.getName().toLowerCase().trim();
                if (!seenStations.contains(normalizedName)) {
                    seenStations.add(normalizedName);
                    allStations.add(station);
                }
            }

            // Small delay between API calls to be respectful
            if (queriesMade < maxQueries && i + stepSize < totalCoords) {
                Thread.sleep(200);
            }
        }

        // Sort by distance from start
        allStations.sort(Comparator.comparingDouble(ChargingStation::getLocationKm));
        return allStations;
    }

    /**
     * Queries OpenChargeMap for stations near a specific coordinate.
     * 
     * API: GET /v3/poi/?latitude=LAT&longitude=LON&distance=5&distanceunit=km&maxresults=5
     */
    private List<ChargingStation> queryOpenChargeMap(double lat, double lon, double waypointKm)
            throws Exception {

        List<ChargingStation> stations = new ArrayList<>();

        String url = String.format(
            "%s?output=json&latitude=%f&longitude=%f&distance=%d&distanceunit=km&maxresults=%d&key=%s",
            ApiConfig.OCM_POI_URL,
            lat, lon,
            ApiConfig.STATION_SEARCH_RADIUS_KM,
            ApiConfig.MAX_STATIONS_PER_QUERY,
            ApiConfig.OCM_API_KEY
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "ChargeFlowV2")
            .GET()
            .timeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return stations; // Return empty — don't crash
        }

        String body = response.body();

        // The response is a JSON array of POI objects
        if (!body.startsWith("[")) return stations;

        List<String> poiList = JsonParser.splitArray(body);

        for (String poi : poiList) {
            try {
                ChargingStation station = parseStation(poi, waypointKm);
                if (station != null) {
                    stations.add(station);
                }
            } catch (Exception e) {
                // Skip malformed station data
            }
        }

        return stations;
    }

    /**
     * Parses a single POI JSON object from OpenChargeMap into a ChargingStation.
     */
    private ChargingStation parseStation(String poiJson, double waypointKm) {
        // Extract station name from AddressInfo
        String addressInfo = JsonParser.getObject(poiJson, "AddressInfo");
        String title = JsonParser.getString(addressInfo, "Title");

        if (title.isEmpty()) {
            title = "Charging Station";
        }

        // Extract coordinates
        double stationLat = JsonParser.getDouble(addressInfo, "Latitude");
        double stationLon = JsonParser.getDouble(addressInfo, "Longitude");

        // Determine charger type from Connections
        String chargerType = "DC Fast Charger";
        String connections = JsonParser.getArray(poiJson, "Connections");
        if (!connections.isEmpty()) {
            // Try to find power level
            double powerKw = JsonParser.getDouble(connections, "PowerKW");
            if (powerKw > 0) {
                if (powerKw >= 50) chargerType = "Fast DC " + (int) powerKw + "kW";
                else if (powerKw >= 22) chargerType = "AC Level 2 " + (int) powerKw + "kW";
                else chargerType = "AC " + (int) powerKw + "kW";
            }
        }

        // Use default price since OCM doesn't always provide pricing
        double price = DEFAULT_PRICE_PER_KWH;

        return new ChargingStation(
            title,
            waypointKm,
            chargerType,
            price,
            stationLat,
            stationLon
        );
    }

    /**
     * Generates fallback charging stations when API is unavailable.
     * Places stations every ~120 km along the route.
     */
    public List<ChargingStation> generateFallbackStations(double totalDistanceKm) {
        List<ChargingStation> stations = new ArrayList<>();

        if (totalDistanceKm <= 0) return stations;

        double intervalKm = 120; // Station every ~120 km
        double positionKm = intervalKm;
        int stationNumber = 1;

        String[] providers = {"Tata Power", "ChargeZone", "EESL", "Statiq", "Ather Grid"};
        String[] chargerTypes = {"Fast DC 50kW", "Fast DC 60kW", "AC Level 2"};
        double[] prices = {15.0, 14.0, 12.0, 16.0, 13.0};

        while (positionKm < totalDistanceKm - 30) {
            int idx = (stationNumber - 1) % providers.length;

            stations.add(new ChargingStation(
                providers[idx] + " - Station " + stationNumber,
                positionKm,
                chargerTypes[idx % chargerTypes.length],
                prices[idx]
            ));

            positionKm += intervalKm;
            stationNumber++;
        }

        return stations;
    }
}
