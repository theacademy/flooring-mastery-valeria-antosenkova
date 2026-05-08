package com.sg.flooringmastery.service;

/**
 * Thrown when we try to add an order but that order number already exists somewhere.
 */
public class FlooringMasteryDuplicateOrderIdException extends Exception {

    public FlooringMasteryDuplicateOrderIdException(String message) {
        super(message);
    }

    // also stores the original exception in case we need to trace back what caused this
    public FlooringMasteryDuplicateOrderIdException(String message, Throwable cause) {
        super(message, cause);
    }
}

