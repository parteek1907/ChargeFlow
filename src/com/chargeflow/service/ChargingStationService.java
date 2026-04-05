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

public class ChargingStationService {

    private final HttpClient httpClient;
    private static final double DEFAULT_PRICE_PER_KWH = 14.0;
    private static final int SEARCH_RADIUS_KM = 40;
    private static final int MAX_RESULTS_PER_QUERY = 10;
    private static final int COORDINATE_STEP = 10;
    private static final int MAX_STATIONS_OUTPUT = 15;

    private static final Set<String> INVALID_NAMES = new HashSet<>(Arrays.asList(
        "public", "private", "unknown", "null", "n/a", "na", "none", "test",
        "charging station", "ev station", "station"
    ));

    public ChargingStationService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(ApiConfig.HTTP_TIMEOUT_SECONDS))
            .build();
    }

    public List<ChargingStation> findStationsAlongRoute(Route route) {
        if (!ApiConfig.isConfigured() || !route.hasCoordinates()) {
            System.out.println("  [INFO] Using estimated charging station data.");
            return generateFallbackStations(route.getDistanceKm());
        }

        try {
            System.out.println("  [STATION] Searching for charging stations along route...");
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

    private List<ChargingStation> fetchStationsFromAPI(Route route) throws Exception {
        List<double[]> coordinates = route.getRouteCoordinates();
        double totalRouteDistance = route.getDistanceKm();
        int totalCoords = coordinates.size();

        if (totalCoords == 0) return new ArrayList<>();

        double[] sourceCoord = coordinates.get(0);
        double sourceLat = sourceCoord[1];
        double sourceLon = sourceCoord[0];

        Set<String> uniqueIds = new HashSet<>();
        List<ChargingStation> allStations = new ArrayList<>();

        int maxQueries = 30;
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
                    + "_" + String.format("%.3f", station.getLatitude())
                    + "_" + String.format("%.3f", station.getLongitude());

                if (uniqueIds.contains(stationId)) continue;
                uniqueIds.add(stationId);

                double distFromSource = haversineKm(
                    sourceLat, sourceLon,
                    station.getLatitude(), station.getLongitude()
                );

                if (distFromSource < 5.0) continue;
                if (distFromSource > totalRouteDistance + 50) continue;

                ChargingStation stationWithDist = new ChargingStation(
                    station.getName(),
                    station.getCity(),
                    station.getState(),
                    station.getCountry(),
                    distFromSource,
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

    private boolean isValidStation(ChargingStation station) {
        if (station == null) return false;

        double lat = station.getLatitude();
        double lon = station.getLongitude();
        if (lat == 0 || lon == 0) return false;
        if (Double.isNaN(lat) || Double.isNaN(lon)) return false;
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return false;

        String name = station.getName();
        if (name == null || name.trim().length() < 3) return false;
        if (INVALID_NAMES.contains(name.toLowerCase().trim())) return false;

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
        if (INVALID_NAMES.contains(nameLower)) return null;

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

    public List<ChargingStation> generateFallbackStations(double totalDistanceKm) {
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
