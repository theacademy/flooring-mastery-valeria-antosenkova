package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Tax;

import java.util.List;

/**
 * Data access interface for state tax rate information.
 * Implementations read state abbreviation, name, and tax rate data from a
 * persistent store (e.g. a flat file).
 */
public interface TaxDao {

    List<Tax> getAllTaxes() throws FlooringMasteryPersistenceException;

    Tax getTaxByState(String stateAbbreviation) throws FlooringMasteryPersistenceException;
}
