package com.sg.flooringmastery.view;

import com.sg.flooringmastery.model.Order;
import com.sg.flooringmastery.model.Product;
import com.sg.flooringmastery.model.Tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Handles all the console output and user input prompts for the app.
 * Uses a UserIO object for the actual reading/printing so it's easy to swap out for testing.
 */
public class FlooringMasteryView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private final UserIO io;

    public FlooringMasteryView(UserIO io) {
        this.io = io;
    }

    public void displayMainMenuBanner() {
        io.print("\n* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
        io.print("* <<Flooring Program>>");
        io.print("* 1. Display Orders");
        io.print("* 2. Add an Order");
        io.print("* 3. Edit an Order");
        io.print("* 4. Remove an Order");
        io.print("* 5. Export All Data");
        io.print("* 6. Quit");
        io.print("*");
        io.print("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
    }

    public int getMenuSelection() {
        while (true) {
            String input = io.readString("Please select from the above choices: ").trim();
            if (input.isEmpty()) {
                // User pressed Enter without typing — silently re-show the prompt.
                // This handles the common case of pressing Enter an extra time
                // after "Press Enter to continue" without printing a confusing error.
                continue;
            }
            try {
                int value = Integer.parseInt(input);
                if (value >= 1 && value <= 6) {
                    return value;
                }
                io.print("Please enter a number between 1 and 6.");
            } catch (NumberFormatException e) {
                io.print("Please enter a number between 1 and 6.");
            }
        }
    }

    // date input

    public LocalDate getDateInput(String prompt) {
        while (true) {
            String input = io.readString(prompt + " (MM-DD-YYYY): ").trim();
            try {
                return LocalDate.parse(input, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                displayErrorMessage("Invalid date format. Please use MM-DD-YYYY.");
            }
        }
    }

    public LocalDate getFutureDateInput() {
        while (true) {
            LocalDate date = getDateInput("Enter order date");
            if (date.isAfter(LocalDate.now())) {
                return date;
            }
            displayErrorMessage("Order date must be in the future. Please try again.");
        }
    }

    // order number input

    public int getOrderNumberInput() {
        return io.readInt("Enter order number: ");
    }

    // customer name input

    public String getCustomerNameInput(String currentName) {
        String prompt;
        if (currentName == null) {
            prompt = "Enter customer name: ";
        } else {
            prompt = "Enter customer name (" + currentName + "): ";
        }
        while (true) {
            String input = io.readString(prompt).trim();
            if (input.isEmpty() && currentName != null) {
                return currentName;
            }
            if (!input.isEmpty() && input.matches("[a-zA-Z0-9 .,]+")) {
                return input;
            }
            if (input.isEmpty()) {
                displayErrorMessage("Customer name may not be blank.");
            } else {
                displayErrorMessage(
                        "Customer name may only contain letters, numbers, spaces, periods, and commas.");
            }
        }
    }

    // state input

    public String getStateInput(List<Tax> taxes, String currentState) {
        io.print("\nAvailable states:");
        for (Tax tax : taxes) {
            io.print(String.format("  %-5s - %-20s  Tax Rate: %.2f%%",
                    tax.getStateAbbreviation(), tax.getStateName(), tax.getTaxRate()));
        }
        String prompt;
        if (currentState == null) {
            prompt = "Enter state abbreviation: ";
        } else {
            prompt = "Enter state abbreviation (" + currentState + "): ";
        }
        while (true) {
            String input = io.readString(prompt).trim().toUpperCase();
            if (input.isEmpty() && currentState != null) {
                return currentState;
            }
            if (!input.isEmpty()) {
                for (Tax t : taxes) {
                    if (t.getStateAbbreviation().equalsIgnoreCase(input)) {
                        return input;
                    }
                }
                displayErrorMessage("State '" + input + "' is not available. Please choose from the list.");
            } else {
                displayErrorMessage("State may not be blank.");
            }
        }
    }

    // product type input

    public String getProductTypeInput(List<Product> products, String currentType) {
        io.print("\nAvailable products:");
        io.print(String.format("  %-15s %-22s %s", "Product Type", "Cost/sq ft", "Labor Cost/sq ft"));
        io.print("  " + "-".repeat(55));
        for (Product p : products) {
            io.print(String.format("  %-15s $%-21s $%s",
                    p.getProductType(),
                    p.getCostPerSquareFoot(),
                    p.getLaborCostPerSquareFoot()));
        }
        String prompt;
        if (currentType == null) {
            prompt = "Enter product type: ";
        } else {
            prompt = "Enter product type (" + currentType + "): ";
        }
        while (true) {
            String input = io.readString(prompt).trim();
            if (input.isEmpty() && currentType != null) {
                return currentType;
            }
            if (!input.isEmpty()) {
                for (Product p : products) {
                    if (p.getProductType().equalsIgnoreCase(input)) {
                        return p.getProductType(); // use the casing from the data file, not however the user typed it
                    }
                }
                displayErrorMessage("Product '" + input + "' is not available. Please choose from the list.");
            } else {
                displayErrorMessage("Product type may not be blank.");
            }
        }
    }

    // area input

    public BigDecimal getAreaInput(BigDecimal currentArea) {
        String prompt;
        if (currentArea == null) {
            prompt = "Enter area (sq ft, minimum 100): ";
        } else {
            prompt = "Enter area (sq ft, minimum 100) (" + currentArea + "): ";
        }
        while (true) {
            String input = io.readString(prompt).trim();
            if (input.isEmpty() && currentArea != null) {
                return currentArea;
            }
            try {
                BigDecimal area = new BigDecimal(input);
                if (area.compareTo(new BigDecimal("100")) >= 0) {
                    return area;
                }
                displayErrorMessage("Area must be at least 100 sq ft.");
            } catch (NumberFormatException e) {
                displayErrorMessage("Please enter a valid decimal number.");
            }
        }
    }

    // display helpers

    public void displayOrderList(List<Order> orders) {
        io.print("\n" + "=".repeat(70));
        io.print(String.format("  %-5s %-20s %-6s %-10s %-8s %s",
                "#", "Name", "State", "Product", "Area", "Total"));
        io.print("  " + "-".repeat(65));
        for (Order o : orders) {
            io.print(String.format("  %-5d %-20s %-6s %-10s %-8s $%s",
                    o.getOrderNumber(),
                    truncate(o.getCustomerName(), 20),
                    o.getState(),
                    o.getProductType(),
                    o.getArea(),
                    o.getTotal()));
        }
        io.print("=".repeat(70));
    }

    public void displayOrderSummary(Order order) {
        io.print("\n--- Order Summary ---");
        io.print(String.format("  Order #:              %d", order.getOrderNumber()));
        io.print(String.format("  Customer Name:        %s", order.getCustomerName()));
        io.print(String.format("  State:                %s", order.getState()));
        io.print(String.format("  Tax Rate:             %.2f%%", order.getTaxRate()));
        io.print(String.format("  Product Type:         %s", order.getProductType()));
        io.print(String.format("  Area:                 %s sq ft", order.getArea()));
        io.print(String.format("  Cost/sq ft:           $%s", order.getCostPerSquareFoot()));
        io.print(String.format("  Labor Cost/sq ft:     $%s", order.getLaborCostPerSquareFoot()));
        io.print(String.format("  Material Cost:        $%s", order.getMaterialCost()));
        io.print(String.format("  Labor Cost:           $%s", order.getLaborCost()));
        io.print(String.format("  Tax:                  $%s", order.getTax()));
        io.print(String.format("  Total:                $%s", order.getTotal()));
        io.print("---------------------");
    }

    // confirmation

    public boolean getYesNoConfirmation(String prompt) {
        while (true) {
            String input = io.readString(prompt + " (y/n): ").trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) return true;
            if (input.equals("n") || input.equals("no"))  return false;
            displayErrorMessage("Please enter y or n.");
        }
    }

    // banners

    public void displayDisplayOrdersBanner() {
        io.print("\n=== Display Orders ===");
    }

    public void displayAddOrderBanner() {
        io.print("\n=== Add an Order ===");
    }

    public void displayEditOrderBanner() {
        io.print("\n=== Edit an Order ===");
    }

    public void displayRemoveOrderBanner() {
        io.print("\n=== Remove an Order ===");
    }

    public void displayExportBanner() {
        io.print("\n=== Export All Data ===");
    }

    // status messages

    public void displaySuccessMessage(String msg) {
        io.print("\nSUCCESS: " + msg);
    }

    public void displayErrorMessage(String msg) {
        io.print("\nERROR: " + msg);
    }

    public void displayExitMessage() {
        io.print("\nGood bye!");
    }

    public void displayPressEnterToContinue() {
        io.readString("\nPress Enter to continue...");
    }

    // utility

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
