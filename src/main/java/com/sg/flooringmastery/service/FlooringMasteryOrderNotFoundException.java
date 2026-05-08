package com.sg.flooringmastery.service;

/**
 * Thrown by the service layer when a requested order cannot be found
 * for the given date and order number combination.
 */
public class FlooringMasteryOrderNotFoundException extends Exception {

    public FlooringMasteryOrderNotFoundException(String message) {
        super(message);
    }

    /** Preserves the original cause so the full stack trace is not lost. */
    public FlooringMasteryOrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
