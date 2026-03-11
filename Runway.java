package org.example;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;

class Runway<T extends Airplane> {
    private String id;
    private boolean isLanding;
    private PriorityQueue<T> airplanesQueue;
    private LocalTime lastUsed;
    private LocalTime lastManeuverTime;
    private final int cooldownPeriod;

    public Runway(String id, boolean isLanding, Comparator<T> priorityComparator) {
        this.id = id;
        this.isLanding = isLanding;
        this.airplanesQueue = new PriorityQueue<>(priorityComparator);
        this.lastUsed = null;
        this.lastManeuverTime = null; // Inițial, nu există manevre
        this.cooldownPeriod = isLanding ? 10 : 5;
    }

    public void addAirplane(Airplane airplane) throws IncorrectRunwayException {
        if (!(airplane instanceof Airplane)) {
            throw new IncorrectRunwayException("Invalid airplane type for this runway.", "Runway and airplane type mismatch");
        }
        airplanesQueue.add((T) airplane);
    }

    public T allowManeuver(LocalTime currentTime) throws UnavailableRunwayException {
        if (lastManeuverTime != null) {
            long minutesSinceLastManeuver = java.time.Duration.between(lastManeuverTime, currentTime).toMinutes();
            if (minutesSinceLastManeuver < cooldownPeriod) {
                throw new UnavailableRunwayException("The chosen runway for maneuver is currently occupied.");
            }
        }
        T airplane = airplanesQueue.poll();
        if (airplane != null) {
            lastManeuverTime = currentTime;
        }
        return airplane;
    }

    public boolean isFree(LocalTime currentTime) {
        if (lastManeuverTime == null) {
            return true;
        }
        LocalTime cooldownEnd = lastManeuverTime.plusMinutes(cooldownPeriod);
        return currentTime.isAfter(cooldownEnd);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(" - ").append(isLanding ? "Landing" : "Takeoff").append(" - ");
        sb.append(lastManeuverTime == null ? "free" : "occupied").append("\n");
        for (T airplane : airplanesQueue) {
            sb.append(airplane.toString()).append("\n");
        }
        return sb.toString();
    }

    public boolean isLanding() {
        return isLanding;
    }

    public List<Airplane> getAirplanesQueue() {
        return new ArrayList<>(this.airplanesQueue);
    }
}
