package org.example;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    private static final Map<String, Runway<?>> runways = new HashMap<>();
    private static final Map<String, Airplane> airplanes = new HashMap<>();
    private static final List<String> exceptionsLog = new ArrayList<>();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Main <test-folder>");
            return;
        }

        String testFolder = args[0];
        String inputFilePath = "src/main/resources/" + testFolder + "/input.in";
        String flightInfoRefPath = "src/main/resources/" + testFolder + "/flight_info.ref";
        String exceptionsRefPath = "src/main/resources/" + testFolder + "/board_exceptions.ref";

        boolean generateFlightInfo = new File(flightInfoRefPath).exists();
        boolean generateExceptions = new File(exceptionsRefPath).exists();

        BufferedWriter flightInfoWriter = null;
        BufferedWriter exceptionsWriter = null;

        try {
            if (generateFlightInfo) {
                String flightInfoFilePath = "src/main/resources/" + testFolder + "/flight_info.out";
                flightInfoWriter = new BufferedWriter(new FileWriter(flightInfoFilePath));
            }

            if (generateExceptions) {
                String exceptionsFilePath = "src/main/resources/" + testFolder + "/board_exceptions.out";
                exceptionsWriter = new BufferedWriter(new FileWriter(exceptionsFilePath));
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
                String command;
                while ((command = reader.readLine()) != null) {
                    processCommand(command.trim(), flightInfoWriter, exceptionsWriter, testFolder);
                }
            }

            if (generateExceptions) {
                for (String exception : exceptionsLog) {
                    exceptionsWriter.write(exception);
                    exceptionsWriter.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        } finally {
            try {
                if (flightInfoWriter != null) {
                    flightInfoWriter.close();
                }
                if (exceptionsWriter != null) {
                    exceptionsWriter.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing writers: " + e.getMessage());
            }
        }
    }

    private static void processCommand(String command, BufferedWriter flightInfoWriter, BufferedWriter exceptionsWriter, String testFolder) {
        try {
            if (command.isEmpty()) return;

            String[] parts = command.split(" - ");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Comanda nu are destule argumente: " + command);
            }

            LocalTime timestamp = LocalTime.parse(parts[0]);
            String action = parts[1];

            switch (action) {
                case "add_runway_in_use" -> addRunway(parts, timestamp, exceptionsWriter);
                case "allocate_plane" -> allocatePlane(parts, timestamp, exceptionsWriter);
                case "permission_for_maneuver" -> permissionForManeuver(parts, timestamp, exceptionsWriter);
                case "runway_info" -> runwayInfo(parts, timestamp, exceptionsWriter, testFolder);
                case "flight_info" -> flightInfo(parts, timestamp, flightInfoWriter);
                case "exit" -> System.out.println("Program terminat");
                default -> throw new IllegalArgumentException("Unknown command: " + action);
            }
        } catch (Exception e) {
            logException(e.getMessage(), LocalTime.now(), exceptionsWriter);
        }
    }

    private static void addRunway(String[] parts, LocalTime timestamp, BufferedWriter exceptionsWriter) {
        if (parts.length < 5) {
            logException("Argumente insuficiente", timestamp, exceptionsWriter);
            return;
        }

        String id = parts[2];
        boolean isLanding = parts[3].equalsIgnoreCase("landing");
        String type = parts[4];

        try {
            Comparator<Airplane> airplaneComparator = Comparator
                    .comparing((Airplane airplane) -> !airplane.isUrgent())
                    .thenComparing(Airplane::getDesiredTime);
            if (type.equalsIgnoreCase("wide body")) {
                runways.put(id, new Runway<>(id, isLanding, airplaneComparator));
            } else if (type.equalsIgnoreCase("narrow body")) {
                runways.put(id, new Runway<>(id, isLanding, airplaneComparator));
            }
            else {
                throw new IllegalArgumentException("Tip de pista invalid: " + type);
            }
        } catch (Exception e) {
            logException(e.getMessage(), timestamp, exceptionsWriter);
        }
    }

    private static void allocatePlane(String[] parts, LocalTime timestamp, BufferedWriter exceptionsWriter) {
        if (parts.length < 9) {
            logException("Argumente insuficiente", timestamp, exceptionsWriter);
            return;
        }

        try {
            String type = parts[2];
            String model = parts[3];
            String flightId = parts[4];
            String departure = parts[5];
            String destination = parts[6];
            LocalTime desiredTime = LocalTime.parse(parts[7]);
            String runwayId = parts[8];
            boolean isUrgent = parts.length > 9 && parts[9].equalsIgnoreCase("urgent");
            Runway<?> runway = runways.get(runwayId);

            if (runway == null) {
                logException("Pista nu există", timestamp, exceptionsWriter);
                return;
            }

            if (!runway.isFree(timestamp)) {
                logException("The chosen runway for maneuver is currently occupied", timestamp, exceptionsWriter);
                return;
            }

            Airplane airplane = type.equalsIgnoreCase("wide body")
                    ? new WideBodyAirplane(model, flightId, departure, destination, desiredTime, isUrgent)
                    : new NarrowBodyAirplane(model, flightId, departure, destination, desiredTime, isUrgent);

            if ((type.equalsIgnoreCase("wide body") && !(runway.getClass().equals(Runway.class))) ||
                    (type.equalsIgnoreCase("narrow body") && !(runway.getClass().equals(Runway.class)))) {
                logException("Nu e compatibil", timestamp, exceptionsWriter);
                return;
            }

            if ((runway.isLanding() && !destination.equalsIgnoreCase("Bucharest")) ||
                    (!runway.isLanding() && !departure.equalsIgnoreCase("Bucharest"))) {
                logException("The chosen runway for allocating the plane is incorrect", timestamp, exceptionsWriter);
                return;
            }

            airplanes.put(flightId, airplane);
            runway.addAirplane(airplane);

        } catch (Exception e) {
            logException(e.getMessage(), timestamp, exceptionsWriter);
        }
    }


    private static void permissionForManeuver(String[] parts, LocalTime timestamp, BufferedWriter exceptionsWriter) {
        if (parts.length < 3) {
            logException("Argumente insuficiente", timestamp, exceptionsWriter);
            return;
        }
        String runwayId = parts[2];
        Runway<?> runway = runways.get(runwayId);
        try {
            Airplane airplane = runway.allowManeuver(timestamp);
            if (airplane != null) {
                airplane.setStatus(runway.isLanding() ? Status.LANDED : Status.DEPARTED);
                airplane.setActualTime(timestamp);
            }
        } catch (UnavailableRunwayException e) {
            logException(e.getMessage(), timestamp, exceptionsWriter);
        }
    }
    private static void runwayInfo(String[] parts, LocalTime timestamp, BufferedWriter exceptionsWriter, String testFolder) {
        if (parts.length < 3) {
            logException("Argumente insuficiente", timestamp, exceptionsWriter);
            return;
        }
        String runwayId = parts[2];
        Runway<?> runway = runways.get(runwayId);
        if (runway == null) {
            logException("Pista nu există", timestamp, exceptionsWriter);
            return;
        }
        List<Airplane> sortedAirplanes = new ArrayList<>(runway.getAirplanesQueue());
        sortedAirplanes.sort(
                Comparator.comparing((Airplane airplane) -> !airplane.isUrgent())
                        .thenComparing(Airplane::getDesiredTime)
        );
        String sanitizedTimestamp = formatTime(timestamp).replace(":", "-");
        String outputFilePath = "src/main/resources/" + testFolder + "/runway_info_" + runwayId + "_" + sanitizedTimestamp + ".out";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            String status = runway.isFree(timestamp) ? "FREE" : "OCCUPIED";
            writer.write(runwayId + " - " + status);
            writer.newLine();
            for (Airplane airplane : sortedAirplanes) {
                writer.write(airplane.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            logException("Eroare la scriere " + e.getMessage(), timestamp, exceptionsWriter);
        }
    }
    private static void flightInfo(String[] parts, LocalTime timestamp, BufferedWriter flightInfoWriter) {
        if (parts.length < 3) {
            logException("Argumente insuficiente", timestamp, flightInfoWriter);
            return;
        }
        String flightId = parts[2];
        Airplane airplane = airplanes.get(flightId);
        try {
            flightInfoWriter.write(String.format("%s | %s%n", formatTime(timestamp), airplane));
        } catch (IOException e) {
            logException("Eroare la scriere: " + e.getMessage(), timestamp, flightInfoWriter);
        }
    }
    private static void logException(String message, LocalTime timestamp, BufferedWriter exceptionsWriter) {
        if (message.endsWith(".")) {
            message = message.substring(0, message.length() - 1);
        }

        String formattedMessage = String.format("%s | %s", formatTime(timestamp), message);
        try {
            exceptionsWriter.write(formattedMessage);
            exceptionsWriter.newLine();
        } catch (IOException e) {
            System.err.println("Eroare la scriere: " + e.getMessage());
        }
    }



    static String formatTime(LocalTime time) {
        return time.format(TIME_FORMATTER);
    }
}