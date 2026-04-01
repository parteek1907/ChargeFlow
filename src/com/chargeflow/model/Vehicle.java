package com.chargeflow.model;

public abstract class Vehicle {

    private final String name;
    private final String type;  

    protected Vehicle(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public abstract double getEfficiency();

    public abstract String describe();
}
