package org.example;
class IncorrectRunwayException extends Exception {
    public IncorrectRunwayException(String message, String runwayAndAirplaneTypeMismatch) {
        super(message);
    }
}