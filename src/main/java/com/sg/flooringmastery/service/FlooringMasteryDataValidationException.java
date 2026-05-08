package com.sg.flooringmastery.service;

/**
 * Thrown by the service layer when user-supplied order data fails
 * business-rule validation (e.g. blank name, unsupported state, area below
 * the minimum, or a date that is not in the future).
 */
public class FlooringMasteryDataValidationException extends Exception {

    public FlooringMasteryDataValidationException(String message) {
        super(message);
    }

    public FlooringMasteryDataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
