package com.sg.flooringmastery.view;

/**
 * Abstraction over console I/O.
 * Separates the view layer from a specific I/O mechanism, making it easy
 * to substitute a test-friendly stub for the real {@code System.in/out}.
 */
public interface UserIO {

    void print(String msg);

    int readInt(String prompt);

    int readInt(String prompt, int min, int max);

    double readDouble(String prompt);

    double readDouble(String prompt, double min, double max);

    String readString(String prompt);
}
