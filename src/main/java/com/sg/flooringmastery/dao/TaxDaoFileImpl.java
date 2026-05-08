package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Tax;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.*;

/**
 * Reads state tax data from a text file.
 * Same idea as ProductDaoFileImpl - load once, cache in memory, reuse after that.
 */
public class TaxDaoFileImpl implements TaxDao {

    private static final String DELIMITER = ",";
    private final String taxesFile;
    // null until loadTaxes() is called for the first time
    private Map<String, Tax> taxes;

    public TaxDaoFileImpl() {
        this.taxesFile = "Data/Taxes.txt";
    }

    public TaxDaoFileImpl(String taxesFile) {
        this.taxesFile = taxesFile;
    }

    private void loadTaxes() throws FlooringMasteryPersistenceException {
        if (taxes != null) return;
        taxes = new LinkedHashMap<>();
        try (Scanner scanner = new Scanner(new FileReader(taxesFile))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine(); // skip header
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] tokens = line.split(DELIMITER);
                    String abbr = tokens[0].trim().toUpperCase();
                    String name = tokens[1].trim();
                    BigDecimal rate = new BigDecimal(tokens[2].trim());
                    taxes.put(abbr, new Tax(abbr, name, rate));
                }
            }
        } catch (FileNotFoundException e) {
            throw new FlooringMasteryPersistenceException(
                    "Could not read taxes file: " + taxesFile, e);
        }
    }

    @Override
    public List<Tax> getAllTaxes() throws FlooringMasteryPersistenceException {
        loadTaxes();
        return new ArrayList<>(taxes.values());
    }

    @Override
    public Tax getTaxByState(String stateAbbreviation) throws FlooringMasteryPersistenceException {
        loadTaxes();
        return taxes.get(stateAbbreviation.toUpperCase());
    }
}
