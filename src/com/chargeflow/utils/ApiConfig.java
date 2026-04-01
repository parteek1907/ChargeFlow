package com.chargeflow.utils;

/**
 * Centralized API configuration.
 * 
 * SETUP: Replace the placeholder keys below with your own free API keys.
 * 1. OpenRouteService → https://openrouteservice.org/dev (sign up → dashboard →
 * request token)
 * 2. OpenChargeMap → https://openchargemap.org/site/develop (sign in → My Apps
 * → register)
 */
public class ApiConfig {

    // ══════════════════════════════════════════════════════════
    // 🔑 PASTE YOUR API KEYS HERE
    // ══════════════════════════════════════════════════════════

    public static final String ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjZlYjFkMzRlNGExZjQyNWY4MjJlMjM3MzUzNzI5MzE0IiwiaCI6Im11cm11cjY0In0=";
    public static final String OCM_API_KEY = "1543263d-42f9-465c-90dc-36ea5b1e64e5";

    // ══════════════════════════════════════════════════════════
    // API Base URLs (do not change)
    // ══════════════════════════════════════════════════════════

    public static final String ORS_GEOCODE_URL = "https://api.openrouteservice.org/geocode/search";
    public static final String ORS_DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/driving-car";
    public static final String OCM_POI_URL = "https://api.openchargemap.io/v3/poi/";

    // ══════════════════════════════════════════════════════════
    // Configuration
    // ══════════════════════════════════════════════════════════

    public static final int HTTP_TIMEOUT_SECONDS = 10;
    public static final String COUNTRY_BIAS = "IN"; // Bias geocoding toward India
    public static final int STATION_SEARCH_RADIUS_KM = 5; // Search radius around route
    public static final int STATION_SAMPLE_INTERVAL_KM = 50; // Sample waypoints every 50 km
    public static final int MAX_STATIONS_PER_QUERY = 5;

    /**
     * Checks if API keys have been configured.
     */
    public static boolean isConfigured() {
        return !ORS_API_KEY.contains("YOUR_") && !OCM_API_KEY.contains("YOUR_");
    }
}
