package com.chargeflow.service;

import com.chargeflow.model.ChargingStation;
import java.util.*;

public class StationService {

    private final List<ChargingStation> allStations;

    public StationService() {
        this.allStations = loadStations();
    }

    public List<ChargingStation> getStationsOnRoute(double totalDistanceKm) {

        List<ChargingStation> onRoute = new ArrayList<>();

        for (ChargingStation station : allStations) {

            if (station.getLocationKm() <= totalDistanceKm && station.getLocationKm() > 0) {
                onRoute.add(station);
            }
        }

        onRoute.sort(Comparator.comparingDouble(ChargingStation::getLocationKm));

        return onRoute;
    }

    public List<ChargingStation> getAllStations() {
        return Collections.unmodifiableList(allStations);
    }

    private List<ChargingStation> loadStations() {

        List<ChargingStation> stations = new ArrayList<>();

        stations.add(new ChargingStation("Tata Power - Manesar",       35,  "Fast DC 50kW",   15.0));
        stations.add(new ChargingStation("EESL - Dharuhera",           60,  "Fast DC 50kW",   12.0));
        stations.add(new ChargingStation("ChargeZone - Neemrana",     122,  "Fast DC 60kW",   14.0));
        stations.add(new ChargingStation("Tata Power - Behror",       144,  "Fast DC 50kW",   15.0));
        stations.add(new ChargingStation("EESL - Shahpura",           200,  "AC Level 2",     10.0));
        stations.add(new ChargingStation("Tata Power - Jaipur Entry", 260,  "Fast DC 60kW",   15.0));

        stations.add(new ChargingStation("ChargeZone - Noida",         20,  "Fast DC 50kW",   14.0));
        stations.add(new ChargingStation("Tata Power - Mathura",      153,  "Fast DC 50kW",   15.0));

        stations.add(new ChargingStation("Tata Power - Panvel",        30,  "Fast DC 60kW",   16.0));
        stations.add(new ChargingStation("ChargeZone - Lonavala",      95,  "Fast DC 50kW",   15.0));

        stations.add(new ChargingStation("EESL - km 300",            300,  "Fast DC 50kW",   12.0));
        stations.add(new ChargingStation("Tata Power - km 450",      450,  "Fast DC 60kW",   15.0));
        stations.add(new ChargingStation("ChargeZone - km 600",      600,  "Fast DC 50kW",   14.0));
        stations.add(new ChargingStation("Tata Power - km 750",      750,  "Fast DC 60kW",   15.0));
        stations.add(new ChargingStation("EESL - km 900",            900,  "Fast DC 50kW",   12.0));
        stations.add(new ChargingStation("ChargeZone - km 1050",    1050,  "Fast DC 50kW",   14.0));
        stations.add(new ChargingStation("Tata Power - km 1200",    1200,  "Fast DC 60kW",   15.0));
        stations.add(new ChargingStation("EESL - km 1350",          1350,  "Fast DC 50kW",   12.0));

        return stations;
    }
}
