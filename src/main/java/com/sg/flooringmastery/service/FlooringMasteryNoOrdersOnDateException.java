package com.sg.flooringmastery.service;

/**
 * Thrown by the service layer when no orders exist for the requested date.
 * The controller catches this and displays an appropriate error message
 * rather than letting the application crash.
 */
public class FlooringMasteryNoOrdersOnDateException extends Exception {

    public FlooringMasteryNoOrdersOnDateException(String message) {
        super(message);
    }
}
