package org.example;

import java.time.LocalTime;

class WideBodyAirplane extends Airplane {
    public WideBodyAirplane(String model, String flightId, String departure, String destination, LocalTime desiredTime, boolean isUrgent) {
        super(model, flightId, departure, destination, desiredTime, isUrgent);
    }

    @Override
    public String toString() {
        return "Wide Body - " + super.toString();
    }
}