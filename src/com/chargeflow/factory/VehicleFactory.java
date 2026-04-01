package com.chargeflow.factory;

import com.chargeflow.model.Vehicle;
import com.chargeflow.model.EVVehicle;
import com.chargeflow.model.ICEVehicle;

public class VehicleFactory {

    public static Vehicle createVehicle(String type) {

        switch (type.toUpperCase()) {

            case "EV":

                return new EVVehicle("Tata Nexon EV", 40.5, 312, 0.13);

            case "ICE":

                return new ICEVehicle("Maruti Suzuki Brezza", "Petrol", 17.38, 48);

            default:
                throw new IllegalArgumentException(
                    "Unknown vehicle type: '" + type + "'. Supported types: EV, ICE"
                );
        }
    }

    public static EVVehicle createCustomEV(String name, double batteryKWh, double rangeKm, double consumptionPerKm) {
        return new EVVehicle(name, batteryKWh, rangeKm, consumptionPerKm);
    }

    public static ICEVehicle createCustomICE(String name, String fuelType, double mileageKmPerL, double tankCapacityL) {
        return new ICEVehicle(name, fuelType, mileageKmPerL, tankCapacityL);
    }
}
