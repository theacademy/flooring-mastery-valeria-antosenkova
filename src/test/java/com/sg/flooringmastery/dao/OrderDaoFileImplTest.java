package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Order;
import org.junit.jupiter.api.*;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderDaoFileImplTest {

    private static final String TEST_ORDERS_DIR = "TestData/TestOrders";
    private static final LocalDate EXISTING_DATE = LocalDate.of(2013, 6, 1);
    private static final LocalDate NEW_DATE = LocalDate.of(2099, 12, 25);

    private OrderDao dao;

    @BeforeEach
    void setUp() {
        dao = new OrderDaoFileImpl(TEST_ORDERS_DIR);
        // Remove any leftover test file for NEW_DATE
        new File(TEST_ORDERS_DIR + "/Orders_12252099.txt").delete();
    }

    @AfterEach
    void tearDown() {
        new File(TEST_ORDERS_DIR + "/Orders_12252099.txt").delete();
    }

    @Test
    void testGetOrdersByDate_existingDate_returnsList() throws Exception {
        List<Order> orders = dao.getOrdersByDate(EXISTING_DATE);
        assertNotNull(orders);
        assertFalse(orders.isEmpty());
    }

    @Test
    void testGetOrdersByDate_noOrdersOnDate_returnsEmptyList() throws Exception {
        List<Order> orders = dao.getOrdersByDate(LocalDate.of(2099, 1, 1));
        assertNotNull(orders);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testGetOrdersByDate_orderHasCorrectData() throws Exception {
        List<Order> orders = dao.getOrdersByDate(EXISTING_DATE);
        Order first = orders.stream()
                .filter(o -> o.getOrderNumber() == 1)
                .findFirst()
                .orElse(null);
        assertNotNull(first);
        assertEquals("Ada Lovelace", first.getCustomerName());
        assertEquals("CA", first.getState());
        assertEquals("Tile", first.getProductType());
        assertEquals(new BigDecimal("249.00"), first.getArea());
        assertEquals(new BigDecimal("2381.06"), first.getTotal());
    }

    @Test
    void testAddOrder_orderSavedAndRetrievable() throws Exception {
        Order order = buildSampleOrder(99, NEW_DATE);
        dao.addOrder(NEW_DATE, order);

        List<Order> orders = dao.getOrdersByDate(NEW_DATE);
        assertEquals(1, orders.size());
        assertEquals(99, orders.get(0).getOrderNumber());
        assertEquals("Jane Doe", orders.get(0).getCustomerName());
    }

    @Test
    void testEditOrder_updatesExistingOrder() throws Exception {
        Order order = buildSampleOrder(99, NEW_DATE);
        dao.addOrder(NEW_DATE, order);

        order.setCustomerName("Updated Name");
        dao.editOrder(NEW_DATE, order);

        List<Order> orders = dao.getOrdersByDate(NEW_DATE);
        assertEquals("Updated Name", orders.get(0).getCustomerName());
    }

    @Test
    void testEditOrder_nonExistentOrder_throwsException() {
        Order order = buildSampleOrder(999, NEW_DATE);
        assertThrows(FlooringMasteryPersistenceException.class,
                () -> dao.editOrder(NEW_DATE, order));
    }

    @Test
    void testRemoveOrder_orderRemovedSuccessfully() throws Exception {
        Order order = buildSampleOrder(99, NEW_DATE);
        dao.addOrder(NEW_DATE, order);

        dao.removeOrder(NEW_DATE, 99);

        List<Order> orders = dao.getOrdersByDate(NEW_DATE);
        assertTrue(orders.isEmpty());
    }

    @Test
    void testRemoveOrder_nonExistentOrder_throwsException() {
        assertThrows(FlooringMasteryPersistenceException.class,
                () -> dao.removeOrder(NEW_DATE, 999));
    }

    @Test
    void testRemoveOrder_lastOrder_deletesFile() throws Exception {
        Order order = buildSampleOrder(99, NEW_DATE);
        dao.addOrder(NEW_DATE, order);

        dao.removeOrder(NEW_DATE, 99);

        File file = new File(TEST_ORDERS_DIR + "/Orders_12252099.txt");
        assertFalse(file.exists());
    }

    @Test
    void testGetAllOrders_returnsMapWithExistingDates() throws Exception {
        Map<LocalDate, List<Order>> allOrders = dao.getAllOrders();
        assertNotNull(allOrders);
        assertTrue(allOrders.containsKey(EXISTING_DATE));
    }

    @Test
    void testCustomerNameWithComma_roundTrip() throws Exception {
        Order order = buildSampleOrder(99, NEW_DATE);
        order.setCustomerName("Acme, Inc.");
        dao.addOrder(NEW_DATE, order);

        List<Order> orders = dao.getOrdersByDate(NEW_DATE);
        assertEquals("Acme, Inc.", orders.get(0).getCustomerName());
    }

    @Test
    void testExportAllData_createsExportFileWithDateColumn() throws Exception {
        String exportPath = "TestData/TestExport.txt";
        new File(exportPath).delete(); // clean slate

        dao.exportAllData(exportPath);

        File exportFile = new File(exportPath);
        assertTrue(exportFile.exists(), "Export file should be created");

        // First line must be the header and must include the OrderDate column
        try (java.util.Scanner sc = new java.util.Scanner(exportFile)) {
            String header = sc.nextLine();
            assertTrue(header.contains("OrderDate"),
                    "Export header must include OrderDate column");
        }

        exportFile.delete(); // cleanup
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Order buildSampleOrder(int number, LocalDate date) {
        Order o = new Order(number, date);
        o.setCustomerName("Jane Doe");
        o.setState("CA");
        o.setTaxRate(new BigDecimal("25.00"));
        o.setProductType("Tile");
        o.setArea(new BigDecimal("150.00"));
        o.setCostPerSquareFoot(new BigDecimal("3.50"));
        o.setLaborCostPerSquareFoot(new BigDecimal("4.15"));
        o.setMaterialCost(new BigDecimal("525.00"));
        o.setLaborCost(new BigDecimal("622.50"));
        o.setTax(new BigDecimal("286.88"));
        o.setTotal(new BigDecimal("1434.38"));
        return o;
    }
}
