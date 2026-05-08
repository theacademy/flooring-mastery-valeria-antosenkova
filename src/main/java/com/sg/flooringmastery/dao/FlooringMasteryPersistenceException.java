package com.sg.flooringmastery.dao;

/**
 * Thrown when the DAO layer cannot read from or write to a data file.
 * Always wraps the underlying {@link java.io.IOException} as the cause
 * so the full stack trace is preserved for debugging.
 */
public class FlooringMasteryPersistenceException extends Exception {

    public FlooringMasteryPersistenceException(String message) {
        super(message);
    }

    public FlooringMasteryPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
