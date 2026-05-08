package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Product;

import java.util.List;

/**
 * Data access interface for flooring product catalogue.
 * Implementations read product type, cost, and labour cost data from a
 * persistent store (e.g. a flat file).
 */
public interface ProductDao {

    List<Product> getAllProducts() throws FlooringMasteryPersistenceException;

    Product getProductByType(String productType) throws FlooringMasteryPersistenceException;
}
