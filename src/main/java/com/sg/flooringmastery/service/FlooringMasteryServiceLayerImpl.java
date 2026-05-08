package com.sg.flooringmastery.service;

import com.sg.flooringmastery.dao.FlooringMasteryPersistenceException;
import com.sg.flooringmastery.dao.OrderDao;
import com.sg.flooringmastery.dao.ProductDao;
import com.sg.flooringmastery.dao.TaxDao;
import com.sg.flooringmastery.model.Order;
import com.sg.flooringmastery.model.Product;
import com.sg.flooringmastery.model.Tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * This is where all the business rules live.
 * Validates input, calculates order costs, and calls the DAOs to save/load data.
 * The controller just calls methods here - it doesn't make decisions on its own.
 */
public class FlooringMasteryServiceLayerImpl implements FlooringMasteryServiceLayer {

    private static final String EXPORT_FILE = "Backup/DataExport.txt";
    private static final BigDecimal MIN_AREA = new BigDecimal("100.00");
    private static final String NAME_PATTERN = "[a-zA-Z0-9 .,]+";

    private final OrderDao orderDao;
    private final ProductDao productDao;
    private final TaxDao taxDao;

    public FlooringMasteryServiceLayerImpl(OrderDao orderDao,
                                            ProductDao productDao,
                                            TaxDao taxDao) {
        this.orderDao = orderDao;
        this.productDao = productDao;
        this.taxDao = taxDao;
    }

    @Override
    public List<Order> getOrdersByDate(LocalDate date)
            throws FlooringMasteryNoOrdersOnDateException, FlooringMasteryPersistenceException {
        List<Order> orders = orderDao.getOrdersByDate(date);
        if (orders.isEmpty()) {
            throw new FlooringMasteryNoOrdersOnDateException(
                    "No orders found for " + date);
        }
        return orders;
    }

    @Override
    public Order addOrder(LocalDate date, Order order)
            throws FlooringMasteryDataValidationException,
                   FlooringMasteryDuplicateOrderIdException,
                   FlooringMasteryPersistenceException {
        validateDate(date);
        validateOrder(order);
        enrichOrderFromTaxAndProduct(order);
        int nextOrderNumber = getNextOrderNumber();
        // just to be safe, make sure this order number isn't already used somewhere
        for (List<Order> existing : orderDao.getAllOrders().values()) {
            for (Order o : existing) {
                if (o.getOrderNumber() == nextOrderNumber) {
                    throw new FlooringMasteryDuplicateOrderIdException(
                            "Order number " + nextOrderNumber + " already exists.");
                }
            }
        }
        order.setOrderNumber(nextOrderNumber);
        order.setOrderDate(date);
        calculateOrderCosts(order);
        return orderDao.addOrder(date, order);
    }

    @Override
    public Order editOrder(LocalDate date, Order order)
            throws FlooringMasteryDataValidationException,
                   FlooringMasteryOrderNotFoundException,
                   FlooringMasteryPersistenceException {
        // make sure the order actually exists before we try to edit it
        Order existing = getOrder(date, order.getOrderNumber());

        // check if any of the editable fields actually changed - if not, nothing to do
        boolean changed = !order.getCustomerName().equals(existing.getCustomerName())
                || !order.getState().equals(existing.getState())
                || !order.getProductType().equals(existing.getProductType())
                || !order.getArea().equals(existing.getArea());

        if (!changed) {
            return existing;
        }

        validateOrder(order);
        enrichOrderFromTaxAndProduct(order);
        order.setOrderDate(date);
        calculateOrderCosts(order);
        try {
            return orderDao.editOrder(date, order);
        } catch (FlooringMasteryPersistenceException e) {
            throw new FlooringMasteryOrderNotFoundException(
                    "Order #" + order.getOrderNumber() + " not found for " + date, e);
        }
    }

    @Override
    public void removeOrder(LocalDate date, int orderNumber)
            throws FlooringMasteryOrderNotFoundException, FlooringMasteryPersistenceException {
        getOrder(date, orderNumber);
        try {
            orderDao.removeOrder(date, orderNumber);
        } catch (FlooringMasteryPersistenceException e) {
            throw new FlooringMasteryOrderNotFoundException(
                    "Order #" + orderNumber + " not found for " + date, e);
        }
    }

    @Override
    public void exportAllData() throws FlooringMasteryPersistenceException {
        orderDao.exportAllData(EXPORT_FILE);
    }

    @Override
    public List<Product> getAllProducts() throws FlooringMasteryPersistenceException {
        return productDao.getAllProducts();
    }

    @Override
    public List<Tax> getAllTaxes() throws FlooringMasteryPersistenceException {
        return taxDao.getAllTaxes();
    }

    @Override
    public Tax getTaxByState(String stateAbbreviation) throws FlooringMasteryPersistenceException {
        return taxDao.getTaxByState(stateAbbreviation);
    }

    @Override
    public Product getProductByType(String productType) throws FlooringMasteryPersistenceException {
        return productDao.getProductByType(productType);
    }

    @Override
    public Order calculateOrderCosts(Order order) throws FlooringMasteryPersistenceException {
        BigDecimal materialCost = order.getArea()
                .multiply(order.getCostPerSquareFoot())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal laborCost = order.getArea()
                .multiply(order.getLaborCostPerSquareFoot())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = materialCost.add(laborCost)
                .multiply(order.getTaxRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal total = materialCost.add(laborCost).add(tax);

        order.setMaterialCost(materialCost);
        order.setLaborCost(laborCost);
        order.setTax(tax);
        order.setTotal(total);
        return order;
    }

    @Override
    public int getNextOrderNumber() throws FlooringMasteryPersistenceException {
        int max = 0;
        for (List<Order> orders : orderDao.getAllOrders().values()) {
            for (Order order : orders) {
                if (order.getOrderNumber() > max) {
                    max = order.getOrderNumber();
                }
            }
        }
        return max + 1;
    }

    @Override
    public Order getOrder(LocalDate date, int orderNumber)
            throws FlooringMasteryOrderNotFoundException, FlooringMasteryPersistenceException {
        List<Order> orders = orderDao.getOrdersByDate(date);
        return orders.stream()
                .filter(o -> o.getOrderNumber() == orderNumber)
                .findFirst()
                .orElseThrow(() -> new FlooringMasteryOrderNotFoundException(
                        "Order #" + orderNumber + " not found for " + date));
    }

    // private helper methods

    /**
     * Looks up the tax rate and product pricing for the given state and product type
     * and sets those values on the order. Throws a validation exception if either one
     * isn't found in the data files.
     */
    private void enrichOrderFromTaxAndProduct(Order order)
            throws FlooringMasteryDataValidationException, FlooringMasteryPersistenceException {
        Tax tax = taxDao.getTaxByState(order.getState());
        if (tax == null) {
            throw new FlooringMasteryDataValidationException(
                    "State '" + order.getState() + "' is not available for orders.");
        }
        Product product = productDao.getProductByType(order.getProductType());
        if (product == null) {
            throw new FlooringMasteryDataValidationException(
                    "Product type '" + order.getProductType() + "' is not available.");
        }
        order.setTaxRate(tax.getTaxRate());
        order.setCostPerSquareFoot(product.getCostPerSquareFoot());
        order.setLaborCostPerSquareFoot(product.getLaborCostPerSquareFoot());
    }

    // date has to be in the future - today doesn't count
    private void validateDate(LocalDate date) throws FlooringMasteryDataValidationException {
        if (!date.isAfter(LocalDate.now())) {
            throw new FlooringMasteryDataValidationException(
                    "Order date must be in the future.");
        }
    }

    // checks that all the fields the user entered are actually valid
    private void validateOrder(Order order) throws FlooringMasteryDataValidationException {
        if (order.getCustomerName() == null || order.getCustomerName().trim().isEmpty()) {
            throw new FlooringMasteryDataValidationException("Customer name may not be blank.");
        }
        if (!order.getCustomerName().matches(NAME_PATTERN)) {
            throw new FlooringMasteryDataValidationException(
                    "Customer name may only contain letters, numbers, spaces, periods, and commas.");
        }
        if (order.getState() == null || order.getState().trim().isEmpty()) {
            throw new FlooringMasteryDataValidationException("State may not be blank.");
        }
        if (order.getProductType() == null || order.getProductType().trim().isEmpty()) {
            throw new FlooringMasteryDataValidationException("Product type may not be blank.");
        }
        if (order.getArea() == null || order.getArea().compareTo(MIN_AREA) < 0) {
            throw new FlooringMasteryDataValidationException(
                    "Area must be at least " + MIN_AREA + " sq ft.");
        }
    }
}
