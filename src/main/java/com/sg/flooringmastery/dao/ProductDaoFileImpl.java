package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Product;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.*;

/**
 * Reads product data from a text file.
 * The file only gets read once and then kept in memory - products don't
 * change while the app is running so there's no reason to re-read every time.
 */
public class ProductDaoFileImpl implements ProductDao {

    private static final String DELIMITER = ",";
    private final String productsFile;
    // null until loadProducts() is called for the first time
    private Map<String, Product> products;

    public ProductDaoFileImpl() {
        this.productsFile = "Data/Products.txt";
    }

    public ProductDaoFileImpl(String productsFile) {
        this.productsFile = productsFile;
    }

    private void loadProducts() throws FlooringMasteryPersistenceException {
        if (products != null) return;
        products = new LinkedHashMap<>();
        try (Scanner scanner = new Scanner(new FileReader(productsFile))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine(); // skip header
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] tokens = line.split(DELIMITER);
                    String type = tokens[0].trim();
                    BigDecimal cost = new BigDecimal(tokens[1].trim());
                    BigDecimal laborCost = new BigDecimal(tokens[2].trim());
                    products.put(type, new Product(type, cost, laborCost));
                }
            }
        } catch (FileNotFoundException e) {
            throw new FlooringMasteryPersistenceException(
                    "Could not read products file: " + productsFile, e);
        }
    }

    @Override
    public List<Product> getAllProducts() throws FlooringMasteryPersistenceException {
        loadProducts();
        return new ArrayList<>(products.values());
    }

    @Override
    public Product getProductByType(String productType) throws FlooringMasteryPersistenceException {
        loadProducts();
        return products.get(productType);
    }
}
