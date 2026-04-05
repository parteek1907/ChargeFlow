package com.chargeflow.utils;

import java.util.ArrayList;
import java.util.List;

public class JsonParser {

    public static String getString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) return "";

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
            valueStart++;
        }

        if (valueStart >= json.length()) return "";

        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd == -1) return "";
            return json.substring(valueStart + 1, valueEnd);
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (c == ',' || c == '}' || c == ']') break;
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd).trim();
    }

    public static double getDouble(String json, String key) {
        String value = getString(json, key);
        if (value.isEmpty()) return -1;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String getArray(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int bracketStart = json.indexOf('[', keyIndex);
        if (bracketStart == -1) return "";

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

    public static List<String> splitArray(String jsonArray) {
        List<String> elements = new ArrayList<>();
        if (jsonArray.isEmpty() || jsonArray.equals("[]")) return elements;

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

        String last = inner.substring(start).trim();
        if (!last.isEmpty()) {
            elements.add(last);
        }

        return elements;
    }

    public static double[] extractCoordinates(String geocodeJson) {
        String coordinates = getArray(geocodeJson, "coordinates");
        if (coordinates.isEmpty()) return null;

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
