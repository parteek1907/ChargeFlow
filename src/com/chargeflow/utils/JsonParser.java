package com.chargeflow.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight JSON parser — extracts values from JSON strings without external libraries.
 * 
 * Why not use Gson/Jackson? Because this project has a strict "pure Java, no frameworks"
 * requirement. This parser handles the specific JSON structures returned by OpenRouteService
 * and OpenChargeMap APIs.
 * 
 * It's NOT a general-purpose JSON parser — it's purpose-built for our API responses.
 */
public class JsonParser {

    /**
     * Extracts a string value for a given key from a JSON string.
     * Finds: "key":"value" or "key": "value"
     * 
     * @param json  The JSON string
     * @param key   The key to search for
     * @return      The value, or empty string if not found
     */
    public static String getString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) return "";

        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
            valueStart++;
        }

        if (valueStart >= json.length()) return "";

        // Check if value is a string (starts with ")
        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd == -1) return "";
            return json.substring(valueStart + 1, valueEnd);
        }

        // Value is not a string — return raw until comma/bracket
        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (c == ',' || c == '}' || c == ']') break;
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd).trim();
    }

    /**
     * Extracts a double value for a given key from a JSON string.
     * 
     * @param json  The JSON string
     * @param key   The key to search for
     * @return      The double value, or -1 if not found/parseable
     */
    public static double getDouble(String json, String key) {
        String value = getString(json, key);
        if (value.isEmpty()) return -1;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Extracts the JSON array value for a given key.
     * Returns the raw string including [ and ].
     * 
     * @param json  The JSON string
     * @param key   The key whose value is an array
     * @return      The array as a raw JSON string, or "" if not found
     */
    public static String getArray(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int bracketStart = json.indexOf('[', keyIndex);
        if (bracketStart == -1) return "";

        // Find matching closing bracket
        int depth = 0;
        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(bracketStart, i + 1);
                }
            }
        }
        return "";
    }

    /**
     * Extracts the JSON object value for a given key.
     * Returns the raw string including { and }.
     * 
     * @param json  The JSON string
     * @param key   The key whose value is an object
     * @return      The object as a raw JSON string, or "" if not found
     */
    public static String getObject(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int braceStart = json.indexOf('{', keyIndex);
        if (braceStart == -1) return "";

        int depth = 0;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(braceStart, i + 1);
                }
            }
        }
        return "";
    }

    /**
     * Splits a JSON array into its top-level elements.
     * Each element is a raw JSON string (either an object or a value).
     * 
     * @param jsonArray  A JSON array string starting with [ and ending with ]
     * @return           List of element strings
     */
    public static List<String> splitArray(String jsonArray) {
        List<String> elements = new ArrayList<>();
        if (jsonArray.isEmpty() || jsonArray.equals("[]")) return elements;

        // Remove outer brackets
        String inner = jsonArray.substring(1, jsonArray.length() - 1).trim();
        if (inner.isEmpty()) return elements;

        int depth = 0;
        int start = 0;

        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                elements.add(inner.substring(start, i).trim());
                start = i + 1;
            }
        }

        // Last element
        String last = inner.substring(start).trim();
        if (!last.isEmpty()) {
            elements.add(last);
        }

        return elements;
    }

    /**
     * Extracts coordinates from an ORS geocode response.
     * Returns [longitude, latitude] or null if not found.
     */
    public static double[] extractCoordinates(String geocodeJson) {
        String coordinates = getArray(geocodeJson, "coordinates");
        if (coordinates.isEmpty()) return null;

        // coordinates is like [77.2090, 28.6139]
        String inner = coordinates.substring(1, coordinates.length() - 1);
        String[] parts = inner.split(",");
        if (parts.length < 2) return null;

        try {
            double lon = Double.parseDouble(parts[0].trim());
            double lat = Double.parseDouble(parts[1].trim());
            return new double[]{lon, lat};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
