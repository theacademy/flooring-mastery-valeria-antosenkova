package com.sg.flooringmastery.view;

import java.util.Scanner;

/**
 * Console implementation of {@link UserIO}.
 * Reads input from {@code System.in} via a {@link Scanner} and writes
 * output to {@code System.out}.  All {@code readInt} / {@code readDouble}
 * methods loop until the user supplies valid input, printing a clear error
 * message (including on a bare Enter press) so the user always knows
 * what is expected.
 */
public class UserIOConsoleImpl implements UserIO {

    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void print(String msg) {
        System.out.println(msg);
    }

    @Override
    public int readInt(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                // Integer.parseInt("") throws NumberFormatException, handled below
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                print("ERROR: Please enter a valid integer.");
                System.out.print(prompt);
                System.out.flush();
            }
        }
    }

    @Override
    public int readInt(String prompt, int min, int max) {
        System.out.print(prompt);
        System.out.flush();
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                // Integer.parseInt("") throws NumberFormatException, handled below
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                print("ERROR: Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                print("ERROR: Please enter a valid integer.");
            }
            System.out.print(prompt);
            System.out.flush();
        }
    }

    @Override
    public double readDouble(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                print("ERROR: Please enter a valid decimal number.");
                System.out.print(prompt);
                System.out.flush();
            }
        }
    }

    @Override
    public double readDouble(String prompt, double min, double max) {
        System.out.print(prompt);
        System.out.flush();
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value >= min && value <= max) {
                    return value;
                }
                print("ERROR: Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                print("ERROR: Please enter a valid decimal number.");
            }
            System.out.print(prompt);
            System.out.flush();
        }
    }

    @Override
    public String readString(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        return scanner.nextLine();
    }
}
