package com.chargeflow.model;

public class EVVehicle extends Vehicle {

    private final double batteryCapacityKWh;      
    private final double rangeKm;                  
    private final double energyConsumptionPerKm;   

    public EVVehicle(String name, double batteryCapacityKWh, double rangeKm, double energyConsumptionPerKm) {
        super(name, "EV");  
        this.batteryCapacityKWh = batteryCapacityKWh;
        this.rangeKm = rangeKm;
        this.energyConsumptionPerKm = energyConsumptionPerKm;
    }

    public double getBatteryCapacityKWh() {
        return batteryCapacityKWh;
    }

    public double getRangeKm() {
        return rangeKm;
    }

    public double getEnergyConsumptionPerKm() {
        return energyConsumptionPerKm;
    }

    @Override
    public double getEfficiency() {
        return rangeKm / batteryCapacityKWh;  
    }

    @Override
    public String describe() {
        return String.format(
            "%s [EV] | Battery: %.1f kWh | Range: %.0f km | Efficiency: %.1f km/kWh",
            getName(), batteryCapacityKWh, rangeKm, getEfficiency()
        );
    }
}
