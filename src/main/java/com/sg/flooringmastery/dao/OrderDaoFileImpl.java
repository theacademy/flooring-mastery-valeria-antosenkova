package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Order;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Reads and writes order files to disk.
 * Each date gets its own file (e.g., Orders_06012013.txt) inside the orders folder.
 * No caching here - every call goes straight to the file so the data is always fresh.
 */
public class OrderDaoFileImpl implements OrderDao {

    private static final String DELIMITER = ",";
    private static final String ORDER_FILE_PREFIX = "Orders_";
    private static final String ORDER_FILE_SUFFIX = ".txt";
    // date format used in the filename, e.g. Orders_06012013.txt
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");
    // date format used in the export file column
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String HEADER =
            "OrderNumber,CustomerName,State,TaxRate,ProductType,Area,"
            + "CostPerSquareFoot,LaborCostPerSquareFoot,MaterialCost,LaborCost,Tax,Total";
    // same header as normal but with an extra OrderDate column at the end
    private static final String EXPORT_HEADER = HEADER + ",OrderDate";

    private final String ordersDir;


    public OrderDaoFileImpl(String ordersDir) {
        this.ordersDir = ordersDir;
    }

    // returns all orders for a given date, or an empty list if no file exists for that date yet
    @Override
    public List<Order> getOrdersByDate(LocalDate date) throws FlooringMasteryPersistenceException {
        File file = getOrderFile(date);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        return loadOrdersFromFile(file, date);
    }

    // reads the existing orders for that date, adds the new one, then writes the whole list back
    @Override
    public Order addOrder(LocalDate date, Order order) throws FlooringMasteryPersistenceException {
        List<Order> orders = getOrdersByDate(date);
        orders.add(order);
        writeOrdersToFile(date, orders);
        return order;
    }

    // finds the matching order by number, replaces it, and saves the file
    // throws an exception if the order number isn't found
    @Override
    public Order editOrder(LocalDate date, Order order) throws FlooringMasteryPersistenceException {
        List<Order> orders = getOrdersByDate(date);
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getOrderNumber() == order.getOrderNumber()) {
                orders.set(i, order);
                writeOrdersToFile(date, orders);
                return order;
            }
        }
        throw new FlooringMasteryPersistenceException(
                "Order #" + order.getOrderNumber() + " not found for date " + date);
    }

    // removes the order from the list - if it was the last one, deletes the file entirely
    // so we don't end up with a bunch of empty files sitting around
    @Override
    public void removeOrder(LocalDate date, int orderNumber) throws FlooringMasteryPersistenceException {
        List<Order> orders = getOrdersByDate(date);
        boolean removed = orders.removeIf(o -> o.getOrderNumber() == orderNumber);
        if (!removed) {
            throw new FlooringMasteryPersistenceException(
                    "Order #" + orderNumber + " not found for date " + date);
        }
        if (orders.isEmpty()) {
            getOrderFile(date).delete();
        } else {
            writeOrdersToFile(date, orders);
        }
    }

    // scans the orders folder for all order files and loads them into a TreeMap
    // TreeMap keeps the dates sorted automatically which is nice for the export
    @Override
    public Map<LocalDate, List<Order>> getAllOrders() throws FlooringMasteryPersistenceException {
        Map<LocalDate, List<Order>> allOrders = new TreeMap<>();
        File dir = new File(ordersDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return allOrders;
        }
        // only grab files that match the Orders_MMDDYYYY.txt naming pattern
        File[] files = dir.listFiles((d, name) ->
                name.startsWith(ORDER_FILE_PREFIX) && name.endsWith(ORDER_FILE_SUFFIX));
        if (files == null) {
            return allOrders;
        }
        for (File file : files) {
            LocalDate date = parseDateFromFilename(file.getName());
            if (date != null) {
                List<Order> orders = loadOrdersFromFile(file, date);
                if (!orders.isEmpty()) {
                    allOrders.put(date, orders);
                }
            }
        }
        return allOrders;
    }

    // loads every order from every date file and writes them all to one backup file
    // adds an OrderDate column so you can tell which date each row came from
    @Override
    public void exportAllData(String exportFilePath) throws FlooringMasteryPersistenceException {
        Map<LocalDate, List<Order>> allOrders = getAllOrders();
        File exportFile = new File(exportFilePath);
        exportFile.getParentFile().mkdirs(); // create the Backup folder if it doesn't exist yet

        try (PrintWriter writer = new PrintWriter(new FileWriter(exportFile))) {
            writer.println(EXPORT_HEADER);
            for (Map.Entry<LocalDate, List<Order>> entry : allOrders.entrySet()) {
                LocalDate date = entry.getKey();
                String dateStr = date.format(EXPORT_DATE_FORMAT);
                for (Order order : entry.getValue()) {
                    writer.println(marshalOrder(order) + DELIMITER + dateStr);
                }
            }
        } catch (IOException e) {
            throw new FlooringMasteryPersistenceException(
                    "Could not write export file: " + exportFilePath, e);
        }
    }

    // builds the file path for a given date, e.g. Orders/Orders_06012013.txt
    private File getOrderFile(LocalDate date) {
        String filename = ORDER_FILE_PREFIX + date.format(FILE_DATE_FORMAT) + ORDER_FILE_SUFFIX;
        return new File(ordersDir + File.separator + filename);
    }

    // strips the prefix and suffix from the filename to get just the date part, then parses it
    // returns null if the filename doesn't match the expected format so we can just skip it
    private LocalDate parseDateFromFilename(String filename) {
        try {
            String datePart = filename
                    .replace(ORDER_FILE_PREFIX, "")
                    .replace(ORDER_FILE_SUFFIX, "");
            return LocalDate.parse(datePart, FILE_DATE_FORMAT);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    // opens the file, skips the header row, and reads each line into an Order object
    private List<Order> loadOrdersFromFile(File file, LocalDate date)
            throws FlooringMasteryPersistenceException {
        List<Order> orders = new ArrayList<>();
        try (Scanner scanner = new Scanner(new FileReader(file))) {
            if (scanner.hasNextLine()) {
                scanner.nextLine(); // skip header
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    Order order = unmarshalOrder(line, date);
                    orders.add(order);
                }
            }
        } catch (FileNotFoundException e) {
            throw new FlooringMasteryPersistenceException(
                    "Could not read orders file: " + file.getPath(), e);
        }
        return orders;
    }

    // creates the orders folder if needed, then writes the header + all orders to the file
    // this overwrites the whole file every time, which keeps things simple
    private void writeOrdersToFile(LocalDate date, List<Order> orders)
            throws FlooringMasteryPersistenceException {
        File dir = new File(ordersDir);
        dir.mkdirs();
        File file = getOrderFile(date);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println(HEADER);
            for (Order order : orders) {
                writer.println(marshalOrder(order));
            }
        } catch (IOException e) {
            throw new FlooringMasteryPersistenceException(
                    "Could not write orders file: " + file.getPath(), e);
        }
    }

    // turns an Order object into a comma-separated string to write to the file
    private String marshalOrder(Order order) {
        return order.getOrderNumber() + DELIMITER
                + order.getCustomerName() + DELIMITER
                + order.getState() + DELIMITER
                + order.getTaxRate() + DELIMITER
                + order.getProductType() + DELIMITER
                + order.getArea() + DELIMITER
                + order.getCostPerSquareFoot() + DELIMITER
                + order.getLaborCostPerSquareFoot() + DELIMITER
                + order.getMaterialCost() + DELIMITER
                + order.getLaborCost() + DELIMITER
                + order.getTax() + DELIMITER
                + order.getTotal();
    }

    // customer names can have commas in them (like "Acme, Inc.") so we can't just split by comma
    private Order unmarshalOrder(String line, LocalDate date) {
        String[] tokens = line.split(DELIMITER);
        // there are always 10 fields after the name, so count backwards to find where the name ends
        int fixedFieldsAfterName = 10; // State through Total
        int nameEndIndex = tokens.length - fixedFieldsAfterName;

        Order order = new Order();
        order.setOrderDate(date);
        order.setOrderNumber(Integer.parseInt(tokens[0].trim()));

        // stitch the name back together in case it had commas in it
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < nameEndIndex; i++) {
            if (i > 1) nameBuilder.append(DELIMITER);
            nameBuilder.append(tokens[i]);
        }
        order.setCustomerName(nameBuilder.toString().trim());

        // now just read the remaining fixed fields in order
        order.setState(tokens[nameEndIndex].trim());
        order.setTaxRate(new BigDecimal(tokens[nameEndIndex + 1].trim()));
        order.setProductType(tokens[nameEndIndex + 2].trim());
        order.setArea(new BigDecimal(tokens[nameEndIndex + 3].trim()));
        order.setCostPerSquareFoot(new BigDecimal(tokens[nameEndIndex + 4].trim()));
        order.setLaborCostPerSquareFoot(new BigDecimal(tokens[nameEndIndex + 5].trim()));
        order.setMaterialCost(new BigDecimal(tokens[nameEndIndex + 6].trim()));
        order.setLaborCost(new BigDecimal(tokens[nameEndIndex + 7].trim()));
        order.setTax(new BigDecimal(tokens[nameEndIndex + 8].trim()));
        order.setTotal(new BigDecimal(tokens[nameEndIndex + 9].trim()));

        return order;
    }
}