package com.sg.flooringmastery.service;

import com.sg.flooringmastery.dao.FlooringMasteryPersistenceException;
import com.sg.flooringmastery.model.Order;
import com.sg.flooringmastery.model.Product;
import com.sg.flooringmastery.model.Tax;

import java.time.LocalDate;
import java.util.List;

/**
 * The service layer interface - this is what the controller calls.
 * All the business rules (validation, cost calculation, etc.) go through here.
 * The controller doesn't make decisions itself, it just calls these methods.
 */
public interface FlooringMasteryServiceLayer {

    List<Order> getOrdersByDate(LocalDate date)
            throws FlooringMasteryNoOrdersOnDateException, FlooringMasteryPersistenceException;

    Order addOrder(LocalDate date, Order order)
            throws FlooringMasteryDataValidationException,
                   FlooringMasteryDuplicateOrderIdException,
                   FlooringMasteryPersistenceException;

    Order editOrder(LocalDate date, Order order)
            throws FlooringMasteryDataValidationException,
                   FlooringMasteryOrderNotFoundException,
                   FlooringMasteryPersistenceException;

    void removeOrder(LocalDate date, int orderNumber)
            throws FlooringMasteryOrderNotFoundException, FlooringMasteryPersistenceException;

    void exportAllData()
            throws FlooringMasteryPersistenceException;

    List<Product> getAllProducts() throws FlooringMasteryPersistenceException;

    List<Tax> getAllTaxes() throws FlooringMasteryPersistenceException;

    Tax getTaxByState(String stateAbbreviation) throws FlooringMasteryPersistenceException;

    Product getProductByType(String productType) throws FlooringMasteryPersistenceException;

    Order calculateOrderCosts(Order order) throws FlooringMasteryPersistenceException;

    int getNextOrderNumber() throws FlooringMasteryPersistenceException;

    Order getOrder(LocalDate date, int orderNumber)
            throws FlooringMasteryOrderNotFoundException, FlooringMasteryPersistenceException;
}
