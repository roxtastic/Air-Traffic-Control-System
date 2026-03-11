package org.example;

import java.time.LocalTime;

class NarrowBodyAirplane extends Airplane {
    public NarrowBodyAirplane(String model, String flightID, String departure, String destination, LocalTime desiredTime, boolean isUrgent) {
        super(model, flightID, departure, destination, desiredTime, isUrgent);
    }

    @Override
    public String toString() {
        return "Narrow Body - " + super.toString();
    }
}