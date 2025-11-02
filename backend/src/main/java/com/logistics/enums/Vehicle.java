package com.logistics.enums;

public class Vehicle {

    public enum VehicleStatus {
        AVAILABLE,
        IN_USE,
        MAINTENANCE,
        ARCHIVED
    }

    public enum VehicleType {
        TRUCK,
        VAN,
        CONTAINER
    }
}