package com.chargeflow.service;

import com.chargeflow.model.ChargingStation;
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

public class ChargingStationService {

    private final HttpClient httpClient;
    private static final double DEFAULT_PRICE_PER_KWH = 14.0;
    private static final int SEARCH_RADIUS_KM = 40;
    private static final int MAX_RESULTS_PER_QUERY = 20;
    private static final int COORDINATE_STEP = 10;
    private static final int MAX_STATIONS_OUTPUT = 30;

    private static final Set<String> INVALID_NAMES = new HashSet<>(Arrays.asList(
        "public", "private", "unknown", "null", "n/a", "na", "none", "test",
        "charging station", "ev station", "station",
        "public - membership required", "public - pay at location",
        "private - for staff, visitors or customers"
    ));

    public ChargingStationService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
            .build();
    }

    public List<ChargingStation> findStationsAlongRoute(Route route) {
        if (!ApiConfig.isConfigured() || !route.hasCoordinates()) {
            System.out.println("  [INFO] Using estimated charging station data.");
            return generateFallbackStations(route);
        }

        try {
            System.out.println("  [STATION] Searching for charging stations along route...");
            List<ChargingStation> stations = fetchStationsFromAPI(route);

            if (stations.isEmpty()) {
                System.out.println("  [WARN] No stations found from API. Using estimated data.");
                return generateFallbackStations(route);
            }

            stations = ensureRouteCoverage(stations, route);

            System.out.println("  [OK] Found " + stations.size() + " charging stations along route.");
            return stations;

        } catch (Exception e) {
            System.out.println("  [WARN] Station API error: " + e.getMessage());
            System.out.println("  [INFO] Using estimated charging station data.");
            return generateFallbackStations(route);
        }
    }

    private List<ChargingStation> fetchStationsFromAPI(Route route) throws Exception {
        List<double[]> coordinates = route.getRouteCoordinates();
        double totalRouteDistance = route.getDistanceKm();
        int totalCoords = coordinates.size();

        if (totalCoords == 0) return new ArrayList<>();

        List<Double> cumulativeRouteKm = buildCumulativeRouteKm(coordinates);

        Set<String> uniqueIds = new HashSet<>();
        List<ChargingStation> allStations = new ArrayList<>();

        int maxQueries = Math.min(60, Math.max(25, (int) Math.ceil(totalRouteDistance / 30.0)));
        int step = Math.max(COORDINATE_STEP, totalCoords / maxQueries);

        int queriesMade = 0;

        System.out.println("  [INFO] Route has " + totalCoords + " coordinate points. Sampling every " + step + " points (max " + maxQueries + " queries)...");

        for (int i = 0; i < totalCoords && queriesMade < maxQueries; i += step) {
            double[] waypoint = coordinates.get(i);
            double lat = waypoint[1];
            double lon = waypoint[0];

            List<ChargingStation> nearby = queryOpenChargeMap(lat, lon);
            queriesMade++;

            for (ChargingStation station : nearby) {
                if (!isValidStation(station)) continue;

                String stationId = station.getName().toLowerCase().trim()
                    + "_" + String.format("%.4f", station.getLatitude())
                    + "_" + String.format("%.4f", station.getLongitude());

                if (uniqueIds.contains(stationId)) continue;
                uniqueIds.add(stationId);

                double routeProgressKm = estimateRouteProgressKm(
                    station.getLatitude(),
                    station.getLongitude(),
                    coordinates,
                    cumulativeRouteKm,
                    totalRouteDistance
                );

                if (routeProgressKm < 5.0) continue;
                if (routeProgressKm > totalRouteDistance + 10.0) continue;

                ChargingStation stationWithDist = new ChargingStation(
                    station.getName(),
                    station.getCity(),
                    station.getState(),
                    station.getCountry(),
                    routeProgressKm,
                    station.getChargerType(),
                    station.getPricePerUnit(),
                    station.getLatitude(),
                    station.getLongitude()
                );
                allStations.add(stationWithDist);
            }

            if (queriesMade < maxQueries && i + step < totalCoords) {
                Thread.sleep(150);
            }
        }

        System.out.println("  [INFO] Queried " + queriesMade + " sample points, found " + allStations.size() + " unique valid stations.");

        allStations.sort(Comparator.comparingDouble(ChargingStation::getLocationKm));

        if (allStations.size() > MAX_STATIONS_OUTPUT) {
            allStations = new ArrayList<>(allStations.subList(0, MAX_STATIONS_OUTPUT));
        }

        return allStations;
    }

    private List<ChargingStation> ensureRouteCoverage(List<ChargingStation> apiStations, Route route) {
        List<ChargingStation> merged = new ArrayList<>(apiStations);
        double totalDistanceKm = route.getDistanceKm();

        int minimumExpected = Math.max(4, (int) Math.floor(totalDistanceKm / 250.0));
        if (merged.size() < minimumExpected) {
            List<ChargingStation> fallback = generateFallbackStations(route);
            merged.addAll(fallback);
        }

        merged = deduplicateByRoutePosition(merged);
        merged.sort(Comparator.comparingDouble(ChargingStation::getLocationKm));

        if (merged.size() > MAX_STATIONS_OUTPUT) {
            merged = selectDistributedStations(merged, totalDistanceKm, MAX_STATIONS_OUTPUT);
        }

        return merged;
    }

    private List<ChargingStation> deduplicateByRoutePosition(List<ChargingStation> stations) {
        List<ChargingStation> sorted = new ArrayList<>(stations);
        sorted.sort(Comparator.comparingDouble(ChargingStation::getLocationKm));

        List<ChargingStation> deduped = new ArrayList<>();
        for (ChargingStation station : sorted) {
            if (deduped.isEmpty()) {
                deduped.add(station);
                continue;
            }

            ChargingStation last = deduped.get(deduped.size() - 1);
            if (Math.abs(station.getLocationKm() - last.getLocationKm()) < 8.0) {
                continue;
            }
            deduped.add(station);
        }

        return deduped;
    }

    private List<ChargingStation> selectDistributedStations(List<ChargingStation> stations,
                                                            double totalDistanceKm,
                                                            int limit) {
        List<ChargingStation> selected = new ArrayList<>();
        if (stations.isEmpty() || limit <= 0) return selected;

        int buckets = Math.min(limit, 10);
        double bucketSize = totalDistanceKm / buckets;

        for (int b = 0; b < buckets; b++) {
            double startKm = b * bucketSize;
            double endKm = (b + 1) * bucketSize;

            ChargingStation best = null;
            for (ChargingStation station : stations) {
                double pos = station.getLocationKm();
                if (pos >= startKm && pos < endKm) {
                    best = station;
                    break;
                }
            }

            if (best != null) {
                selected.add(best);
            }
        }

        if (selected.isEmpty()) {
            return new ArrayList<>(stations.subList(0, Math.min(limit, stations.size())));
        }

        for (ChargingStation station : stations) {
            if (selected.size() >= limit) break;
            if (!selected.contains(station)) {
                selected.add(station);
            }
        }

        selected.sort(Comparator.comparingDouble(ChargingStation::getLocationKm));
        return selected;
    }

    private List<Double> buildCumulativeRouteKm(List<double[]> coordinates) {
        List<Double> cumulative = new ArrayList<>();
        cumulative.add(0.0);

        double total = 0.0;
        for (int i = 1; i < coordinates.size(); i++) {
            double[] prev = coordinates.get(i - 1);
            double[] curr = coordinates.get(i);
            total += haversineKm(prev[1], prev[0], curr[1], curr[0]);
            cumulative.add(total);
        }

        return cumulative;
    }

    private double estimateRouteProgressKm(double stationLat, double stationLon,
                                           List<double[]> routeCoords,
                                           List<Double> cumulativeRouteKm,
                                           double totalRouteDistanceKm) {
        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < routeCoords.size(); i++) {
            double[] coord = routeCoords.get(i);
            double distance = haversineKm(stationLat, stationLon, coord[1], coord[0]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        double routeKmAtIndex = cumulativeRouteKm.get(bestIndex);
        double routeLengthFromCoords = cumulativeRouteKm.get(cumulativeRouteKm.size() - 1);

        if (routeLengthFromCoords <= 0) {
            return routeKmAtIndex;
        }

        double scaled = (routeKmAtIndex / routeLengthFromCoords) * totalRouteDistanceKm;
        return Math.max(0.0, Math.min(totalRouteDistanceKm, scaled));
    }

    private boolean isValidStation(ChargingStation station) {
        if (station == null) return false;

        double lat = station.getLatitude();
        double lon = station.getLongitude();
        if (lat == 0 || lon == 0) return false;
        if (Double.isNaN(lat) || Double.isNaN(lon)) return false;
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return false;

        String name = station.getName();
        if (name == null || name.trim().length() < 3) return false;
        String normalizedName = name.toLowerCase().trim();
        if (isGenericNonStationName(normalizedName)) return false;

        return true;
    }

    private List<ChargingStation> queryOpenChargeMap(double lat, double lon) throws Exception {
        List<ChargingStation> stations = new ArrayList<>();

        String url = String.format(
            "%s?output=json&latitude=%f&longitude=%f&distance=%d&distanceunit=km&maxresults=%d&key=%s",
            ApiConfig.OCM_POI_URL,
            lat, lon,
            SEARCH_RADIUS_KM,
            MAX_RESULTS_PER_QUERY,
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
            return stations;
        }

        String body = response.body();

        if (!body.startsWith("[")) return stations;

        List<String> poiList = JsonParser.splitArray(body);

        for (String poi : poiList) {
            try {
                ChargingStation station = parseStation(poi);
                if (station != null) {
                    stations.add(station);
                }
            } catch (Exception e) {
            }
        }

        return stations;
    }

    private ChargingStation parseStation(String poiJson) {
        String addressInfo = JsonParser.getObject(poiJson, "AddressInfo");
        if (addressInfo.isEmpty()) return null;

        String title = JsonParser.getString(addressInfo, "Title");
        if (title.isEmpty() || title.trim().length() < 3) return null;

        String nameLower = title.toLowerCase().trim();
        if (isGenericNonStationName(nameLower)) return null;

        String city = JsonParser.getString(addressInfo, "Town");
        if (city == null || city.isEmpty() || city.equalsIgnoreCase("null")) {
            city = "Unknown";
        }

        String state = JsonParser.getString(addressInfo, "StateOrProvince");
        if (state == null || state.equalsIgnoreCase("null")) {
            state = "";
        }

        String countryInfo = JsonParser.getObject(addressInfo, "Country");
        String country = JsonParser.getString(countryInfo, "Title");
        if (country == null || country.equalsIgnoreCase("null")) {
            country = "";
        }

        double stationLat = JsonParser.getDouble(addressInfo, "Latitude");
        double stationLon = JsonParser.getDouble(addressInfo, "Longitude");

        if (stationLat == 0 || stationLon == 0) return null;
        if (Double.isNaN(stationLat) || Double.isNaN(stationLon)) return null;
        if (stationLat < -90 || stationLat > 90 || stationLon < -180 || stationLon > 180) return null;

        String chargerType = "DC Fast Charger";
        String connections = JsonParser.getArray(poiJson, "Connections");
        if (!connections.isEmpty()) {
            double powerKw = JsonParser.getDouble(connections, "PowerKW");
            if (powerKw > 0) {
                if (powerKw >= 50) chargerType = "Fast DC " + (int) powerKw + "kW";
                else if (powerKw >= 22) chargerType = "AC Level 2 " + (int) powerKw + "kW";
                else chargerType = "AC " + (int) powerKw + "kW";
            }
        }

        return new ChargingStation(
            title.trim(),
            city,
            state,
            country,
            0,
            chargerType,
            DEFAULT_PRICE_PER_KWH,
            stationLat,
            stationLon
        );
    }

    private boolean isGenericNonStationName(String normalizedName) {
        if (INVALID_NAMES.contains(normalizedName)) return true;
        if (normalizedName.startsWith("public -") || normalizedName.startsWith("private -")) return true;
        if (normalizedName.contains("membership required")) return true;
        if (normalizedName.contains("pay at location")) return true;
        if (normalizedName.contains("for staff, visitors or customers")) return true;
        return false;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public List<ChargingStation> generateFallbackStations(Route route) {
        if (!route.hasCoordinates() || !ApiConfig.isConfigured()) {
            return generateFallbackStations(route.getDistanceKm(), route.getSource() + " - " + route.getDestination() + " corridor", "India");
        }

        List<ChargingStation> stations = new ArrayList<>();
        List<double[]> coordinates = route.getRouteCoordinates();
        List<Double> cumulativeRouteKm = buildCumulativeRouteKm(coordinates);

        Map<String, String> cityCache = new HashMap<>();
        double totalDistanceKm = route.getDistanceKm();

        double intervalKm = 120;
        double positionKm = intervalKm;
        int stationNumber = 1;

        String[] providers = {"Tata Power", "ChargeZone", "EESL", "Statiq", "Ather Grid"};
        String[] chargerTypes = {"Fast DC 50kW", "Fast DC 60kW", "AC Level 2"};
        double[] prices = {15.0, 14.0, 12.0, 16.0, 13.0};

        while (positionKm < totalDistanceKm - 30) {
            int idx = (stationNumber - 1) % providers.length;

            double[] coord = getCoordinateAtRouteKm(coordinates, cumulativeRouteKm, totalDistanceKm, positionKm);
            String city = reverseGeocodeCity(coord[1], coord[0], route.getSource(), route.getDestination(), cityCache);

            stations.add(new ChargingStation(
                providers[idx] + " - Station " + stationNumber,
                city,
                "",
                "India",
                positionKm,
                chargerTypes[idx % chargerTypes.length],
                prices[idx],
                coord[1],
                coord[0]
            ));

            positionKm += intervalKm;
            stationNumber++;
        }

        return stations;
    }

    public List<ChargingStation> generateFallbackStations(double totalDistanceKm, String cityLabel, String countryLabel) {
        List<ChargingStation> stations = new ArrayList<>();

        if (totalDistanceKm <= 0) return stations;

        double intervalKm = 120;
        double positionKm = intervalKm;
        int stationNumber = 1;

        String[] providers = {"Tata Power", "ChargeZone", "EESL", "Statiq", "Ather Grid"};
        String[] chargerTypes = {"Fast DC 50kW", "Fast DC 60kW", "AC Level 2"};
        double[] prices = {15.0, 14.0, 12.0, 16.0, 13.0};

        while (positionKm < totalDistanceKm - 30) {
            int idx = (stationNumber - 1) % providers.length;

            stations.add(new ChargingStation(
                providers[idx] + " - Station " + stationNumber,
                cityLabel,
                "",
                countryLabel,
                positionKm,
                chargerTypes[idx % chargerTypes.length],
                prices[idx],
                0,
                0
            ));

            positionKm += intervalKm;
            stationNumber++;
        }

        return stations;
    }

    private double[] getCoordinateAtRouteKm(List<double[]> routeCoords,
                                            List<Double> cumulativeRouteKm,
                                            double totalRouteDistanceKm,
                                            double targetKm) {
        if (routeCoords == null || routeCoords.isEmpty()) {
            return new double[]{0, 0};
        }

        double routeLengthFromCoords = cumulativeRouteKm.get(cumulativeRouteKm.size() - 1);
        if (routeLengthFromCoords <= 0 || totalRouteDistanceKm <= 0) {
            return routeCoords.get(Math.max(0, Math.min(routeCoords.size() - 1, routeCoords.size() / 2)));
        }

        double targetCoordKm = (targetKm / totalRouteDistanceKm) * routeLengthFromCoords;

        int bestIndex = 0;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < cumulativeRouteKm.size(); i++) {
            double diff = Math.abs(cumulativeRouteKm.get(i) - targetCoordKm);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIndex = i;
            }
        }

        return routeCoords.get(bestIndex);
    }

    private String reverseGeocodeCity(double lat, double lon,
                                      String defaultSource,
                                      String defaultDestination,
                                      Map<String, String> cache) {
        String key = String.format("%.3f_%.3f", lat, lon);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        try {
            String url = String.format(
                "https://api.openrouteservice.org/geocode/reverse?api_key=%s&point.lon=%s&point.lat=%s&size=1",
                URLEncoder.encode(ApiConfig.ORS_API_KEY, StandardCharsets.UTF_8),
                URLEncoder.encode(String.valueOf(lon), StandardCharsets.UTF_8),
                URLEncoder.encode(String.valueOf(lat), StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String fallback = defaultSource + " - " + defaultDestination + " corridor";
                cache.put(key, fallback);
                return fallback;
            }

            String body = response.body();
            String locality = JsonParser.getString(body, "locality");
            String city = JsonParser.getString(body, "city");
            String county = JsonParser.getString(body, "county");
            String region = JsonParser.getString(body, "region");

            String resolved = pickBestLocationLabel(locality, city, county, region,
                defaultSource + " - " + defaultDestination + " corridor");

            cache.put(key, resolved);
            return resolved;
        } catch (Exception e) {
            String fallback = defaultSource + " - " + defaultDestination + " corridor";
            cache.put(key, fallback);
            return fallback;
        }
    }

    private String pickBestLocationLabel(String locality,
                                         String city,
                                         String county,
                                         String region,
                                         String fallback) {
        List<String> candidates = Arrays.asList(locality, city, county, region);
        for (String candidate : candidates) {
            if (isValidLocationToken(candidate)) {
                return candidate.trim();
            }
        }
        return fallback;
    }

    private boolean isValidLocationToken(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) return false;
        if (normalized.equals("unknown") || normalized.equals("null") || normalized.equals("n/a") || normalized.equals("na")) return false;
        if (normalized.contains("whosonfirst")) return false;
        if (normalized.contains("geonames")) return false;
        if (normalized.contains("openstreetmap")) return false;
        return true;
    }
}
