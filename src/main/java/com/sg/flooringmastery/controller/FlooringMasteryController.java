package com.sg.flooringmastery.controller;

import com.sg.flooringmastery.dao.FlooringMasteryPersistenceException;
import com.sg.flooringmastery.model.Order;
import com.sg.flooringmastery.model.Product;
import com.sg.flooringmastery.model.Tax;
import com.sg.flooringmastery.service.*;
import com.sg.flooringmastery.view.FlooringMasteryView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Runs the main menu loop and wires together the view and the service.
 * Each menu option has its own private method that handles that specific flow.
 */
public class FlooringMasteryController {

    private final FlooringMasteryServiceLayer service;
    private final FlooringMasteryView view;

    public FlooringMasteryController(FlooringMasteryServiceLayer service,
                                      FlooringMasteryView view) {
        this.service = service;
        this.view = view;
    }

    // main loop - keeps going until the user picks quit
    public void run() {
        boolean running = true;
        while (running) {
            view.displayMainMenuBanner();
            int choice = view.getMenuSelection();
            switch (choice) {
                case 1 -> displayOrders();
                case 2 -> addOrder();
                case 3 -> editOrder();
                case 4 -> removeOrder();
                case 5 -> exportData();
                case 6 -> {
                    view.displayExitMessage();
                    running = false;
                }
            }
        }
    }

    // display orders

    private void displayOrders() {
        view.displayDisplayOrdersBanner();
        LocalDate date = view.getDateInput("Enter the date to display orders for");
        try {
            List<Order> orders = service.getOrdersByDate(date);
            view.displayOrderList(orders);
        } catch (FlooringMasteryNoOrdersOnDateException e) {
            view.displayErrorMessage(e.getMessage());
        } catch (FlooringMasteryPersistenceException e) {
            view.displayErrorMessage("Could not load orders: " + e.getMessage());
        }
        view.displayPressEnterToContinue();
    }

    // add order

    private void addOrder() {
        view.displayAddOrderBanner();
        try {
            LocalDate date = view.getFutureDateInput();

            List<Tax> taxes = service.getAllTaxes();
            List<Product> products = service.getAllProducts();

            String customerName = view.getCustomerNameInput(null);
            String state = view.getStateInput(taxes, null);
            String productType = view.getProductTypeInput(products, null);
            BigDecimal area = view.getAreaInput(null);

            // build the order with pricing filled in so we can show the user a preview
            Order order = new Order();
            order.setCustomerName(customerName);
            order.setState(state);
            order.setProductType(productType);
            order.setArea(area);
            order.setOrderNumber(service.getNextOrderNumber());
            order.setOrderDate(date);
            enrichAndCalculatePreview(order);

            view.displayOrderSummary(order);

            if (view.getYesNoConfirmation("Place this order?")) {
                service.addOrder(date, order);
                view.displaySuccessMessage("Order #" + order.getOrderNumber() + " added successfully.");
            } else {
                view.displayErrorMessage("Order was not saved.");
            }

        } catch (FlooringMasteryDuplicateOrderIdException | FlooringMasteryDataValidationException e) {
            view.displayErrorMessage(e.getMessage());
        } catch (FlooringMasteryPersistenceException e) {
            view.displayErrorMessage("Could not save order: " + e.getMessage());
        }
        view.displayPressEnterToContinue();
    }

    // edit order

    private void editOrder() {
        view.displayEditOrderBanner();
        try {
            LocalDate date = view.getDateInput("Enter the date of the order to edit");
            int orderNumber = view.getOrderNumberInput();

            Order existing = service.getOrder(date, orderNumber);

            List<Tax> taxes = service.getAllTaxes();
            List<Product> products = service.getAllProducts();

            String customerName = view.getCustomerNameInput(existing.getCustomerName());
            String state = view.getStateInput(taxes, existing.getState());
            String productType = view.getProductTypeInput(products, existing.getProductType());
            BigDecimal area = view.getAreaInput(existing.getArea());

            // build an updated order with whatever the user changed and show a preview before saving
            Order updated = new Order(existing.getOrderNumber(), date);
            updated.setCustomerName(customerName);
            updated.setState(state);
            updated.setProductType(productType);
            updated.setArea(area);
            enrichAndCalculatePreview(updated);

            view.displayOrderSummary(updated);

            if (view.getYesNoConfirmation("Save changes?")) {
                service.editOrder(date, updated);
                view.displaySuccessMessage("Order #" + orderNumber + " updated successfully.");
            } else {
                view.displayErrorMessage("Changes were not saved.");
            }

        } catch (FlooringMasteryOrderNotFoundException e) {
            view.displayErrorMessage(e.getMessage());
        } catch (FlooringMasteryDataValidationException e) {
            view.displayErrorMessage(e.getMessage());
        } catch (FlooringMasteryPersistenceException e) {
            view.displayErrorMessage("Could not edit order: " + e.getMessage());
        }
        view.displayPressEnterToContinue();
    }

    // remove order

    private void removeOrder() {
        view.displayRemoveOrderBanner();
        try {
            LocalDate date = view.getDateInput("Enter the date of the order to remove");
            int orderNumber = view.getOrderNumberInput();

            Order order = service.getOrder(date, orderNumber);
            view.displayOrderSummary(order);

            if (view.getYesNoConfirmation("Are you sure you want to remove this order?")) {
                service.removeOrder(date, orderNumber);
                view.displaySuccessMessage("Order #" + orderNumber + " removed successfully.");
            } else {
                view.displayErrorMessage("Order was not removed.");
            }

        } catch (FlooringMasteryOrderNotFoundException e) {
            view.displayErrorMessage(e.getMessage());
        } catch (FlooringMasteryPersistenceException e) {
            view.displayErrorMessage("Could not remove order: " + e.getMessage());
        }
        view.displayPressEnterToContinue();
    }

    // export all data

    private void exportData() {
        view.displayExportBanner();
        try {
            service.exportAllData();
            view.displaySuccessMessage("All data exported to Backup/DataExport.txt.");
        } catch (FlooringMasteryPersistenceException e) {
            view.displayErrorMessage("Export failed: " + e.getMessage());
        }
        view.displayPressEnterToContinue();
    }

    // private helper methods

    /**
     * Fills in the tax rate and product pricing on an order and calculates the costs.
     * Used in both add and edit so we don't repeat the same four lines twice.
     */
    private void enrichAndCalculatePreview(Order order) throws FlooringMasteryPersistenceException {
        Tax tax = service.getTaxByState(order.getState());
        Product product = service.getProductByType(order.getProductType());
        order.setTaxRate(tax.getTaxRate());
        order.setCostPerSquareFoot(product.getCostPerSquareFoot());
        order.setLaborCostPerSquareFoot(product.getLaborCostPerSquareFoot());
        service.calculateOrderCosts(order);
    }
}
