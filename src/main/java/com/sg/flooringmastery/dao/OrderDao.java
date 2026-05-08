package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Order;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interface for reading and writing orders.
 * Orders are organized by date - each date can have a list of orders associated with it.
 */
public interface OrderDao {

    List<Order> getOrdersByDate(LocalDate date) throws FlooringMasteryPersistenceException;

    Order addOrder(LocalDate date, Order order) throws FlooringMasteryPersistenceException;

    Order editOrder(LocalDate date, Order order) throws FlooringMasteryPersistenceException;

    void removeOrder(LocalDate date, int orderNumber) throws FlooringMasteryPersistenceException;

    Map<LocalDate, List<Order>> getAllOrders() throws FlooringMasteryPersistenceException;

    void exportAllData(String exportFilePath) throws FlooringMasteryPersistenceException;
}
