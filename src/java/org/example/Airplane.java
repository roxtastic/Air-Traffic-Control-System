package org.example;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.example.Main.formatTime;

enum Status {
    WAITING_FOR_TAKEOFF,
    DEPARTED,
    WAITING_FOR_LANDING,
    LANDED
}

class Airplane {
    private String model;
    private String flightId;
    private String departure;
    private String destination;
    private LocalTime desiredTime;
    private LocalTime actualTime;
    private Status status;
    private boolean isUrgent = false;

    public Airplane(String model, String flightId, String departure, String destination, LocalTime desiredTime, boolean isUrgent) {
        this.model = model;
        this.flightId = flightId;
        this.departure = departure;
        this.destination = destination;
        this.desiredTime = desiredTime;
        this.status = determineInitialStatus(departure, destination);
        this.isUrgent = isUrgent;
    }

    private Status determineInitialStatus(String departure, String destination) {
        return destination.equals("Bucharest") ? Status.WAITING_FOR_LANDING : Status.WAITING_FOR_TAKEOFF;
    }

    public void setActualTime(LocalTime actualTime) {
        this.actualTime = actualTime;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("%s - %s - %s - %s - %s - %s%s",
                model, flightId, departure, destination, status, formatTime(desiredTime),
                (actualTime != null ? " - " + formatTime(actualTime) : ""));
    }
    public String getDestination() {
        return destination;
    }
    public LocalTime getDesiredTime() {
        return desiredTime;
    }
    public boolean isUrgent() {
        return isUrgent;
    }
    public LocalTime getActualTime() {
        return actualTime;
    }

}
