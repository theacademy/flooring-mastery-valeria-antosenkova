package com.sg.flooringmastery.service;

import com.sg.flooringmastery.dao.FlooringMasteryPersistenceException;
import com.sg.flooringmastery.dao.OrderDao;
import com.sg.flooringmastery.dao.ProductDao;
import com.sg.flooringmastery.dao.TaxDao;
import com.sg.flooringmastery.model.Order;
import com.sg.flooringmastery.model.Product;
import com.sg.flooringmastery.model.Tax;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlooringMasteryServiceLayerImplTest {

    @Mock private OrderDao orderDao;
    @Mock private ProductDao productDao;
    @Mock private TaxDao taxDao;

    private FlooringMasteryServiceLayer service;

    // Future date used across tests
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(30);
    private static final LocalDate PAST_DATE   = LocalDate.now().minusDays(1);

    @BeforeEach
    void setUp() {
        service = new FlooringMasteryServiceLayerImpl(orderDao, productDao, taxDao);
    }

    // ── getOrdersByDate ───────────────────────────────────────────────────────

    @Test
    void testGetOrdersByDate_ordersExist_returnsList() throws Exception {
        List<Order> orders = List.of(buildOrder(1, FUTURE_DATE));
        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(orders);

        List<Order> result = service.getOrdersByDate(FUTURE_DATE);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetOrdersByDate_noOrders_throwsNoOrdersException() throws Exception {
        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(Collections.emptyList());

        assertThrows(FlooringMasteryNoOrdersOnDateException.class,
                () -> service.getOrdersByDate(FUTURE_DATE));
    }

    // ── addOrder ──────────────────────────────────────────────────────────────

    @Test
    void testAddOrder_validOrder_savedAndReturned() throws Exception {
        Order order = buildOrder(0, FUTURE_DATE);
        Tax tax = new Tax("CA", "California", new BigDecimal("25.00"));
        Product product = new Product("Tile", new BigDecimal("3.50"), new BigDecimal("4.15"));

        // arrange - build input data, create any mock behaviors
        when(taxDao.getTaxByState("CA")).thenReturn(tax);
        when(productDao.getProductByType("Tile")).thenReturn(product);
        when(orderDao.getAllOrders()).thenReturn(new TreeMap<>());
        when(orderDao.addOrder(eq(FUTURE_DATE), any(Order.class))).thenAnswer(i -> i.getArgument(1));

        // act - call the method we test
        Order saved = service.addOrder(FUTURE_DATE, order);

        // assert - check the result is what we expect
        assertNotNull(saved);
        assertEquals(1, saved.getOrderNumber()); // next number after empty store
        assertNotNull(saved.getMaterialCost());
        assertNotNull(saved.getTotal());
        verify(orderDao).addOrder(eq(FUTURE_DATE), any(Order.class));
    }

    @Test
    void testAddOrder_pastDate_throwsValidationException() {
        Order order = buildOrder(0, PAST_DATE);

        assertThrows(FlooringMasteryDataValidationException.class,
                () -> service.addOrder(PAST_DATE, order));
        verifyNoInteractions(orderDao);
    }

    @Test
    void testAddOrder_todayDate_throwsValidationException() {
        Order order = buildOrder(0, LocalDate.now());

        assertThrows(FlooringMasteryDataValidationException.class,
                () -> service.addOrder(LocalDate.now(), order));
    }

    @Test
    void testAddOrder_blankCustomerName_throwsValidationException() throws Exception {
        Order order = buildOrder(0, FUTURE_DATE);
        order.setCustomerName("");

        assertThrows(FlooringMasteryDataValidationException.class,
                () -> service.addOrder(FUTURE_DATE, order));
    }

    @Test
    void testAddOrder_invalidCharactersInName_throwsValidationException() throws Exception {
        Order order = buildOrder(0, FUTURE_DATE);
        order.setCustomerName("Ada@Lovelace!");

        assertThrows(FlooringMasteryDataValidationException.class,
                () -> service.addOrder(FUTURE_DATE, order));
    }

    @Test
    void testAddOrder_nameWithCommaAndPeriod_isValid() throws Exception {
        Order order = buildOrder(0, FUTURE_DATE);
        order.setCustomerName("Acme, Inc.");

        Tax tax = new Tax("CA", "California", new BigDecimal("25.00"));
        Product product = new Product("Tile", new BigDecimal("3.50"), new BigDecimal("4.15"));

        when(taxDao.getTaxByState("CA")).thenReturn(tax);
        when(productDao.getProductByType("Tile")).thenReturn(product);
        when(orderDao.getAllOrders()).thenReturn(new TreeMap<>());
        when(orderDao.addOrder(eq(FUTURE_DATE), any(Order.class))).thenAnswer(i -> i.getArgument(1));

        assertDoesNotThrow(() -> service.addOrder(FUTURE_DATE, order));
    }

    @Test
    void testAddOrder_areaBelowMinimum_throwsValidationException() throws Exception {
        Order order = buildOrder(0, FUTURE_DATE);
        order.setArea(new BigDecimal("99.99"));

        assertThrows(FlooringMasteryDataValidationException.class,
                () -> service.addOrder(FUTURE_DATE, order));
    }

    @Test
    void testAddOrder_stateNotInTaxFile_throwsValidationException() throws Exception {
        Order order = buildOrder(0, FUTURE_DATE);
        when(taxDao.getTaxByState("ZZ")).thenReturn(null);
        order.setState("ZZ");

        assertThrows(FlooringMasteryDataValidationException.class,
                () -> service.addOrder(FUTURE_DATE, order));
    }

    @Test
    void testAddOrder_productNotInProductFile_throwsValidationException() throws Exception {
        Order order = buildOrder(0, FUTURE_DATE);
        Tax tax = new Tax("CA", "California", new BigDecimal("25.00"));
        when(taxDao.getTaxByState("CA")).thenReturn(tax);
        when(productDao.getProductByType("Tile")).thenReturn(null);

        assertThrows(FlooringMasteryDataValidationException.class,
                () -> service.addOrder(FUTURE_DATE, order));
    }

    // ── calculateOrderCosts ────────────────────────────────────────────────────

    @Test
    void testCalculateOrderCosts_correctValues() throws Exception {
        Order order = buildOrder(1, FUTURE_DATE);
        // area=249, costPerSqFt=3.50, laborPerSqFt=4.15, taxRate=25.00
        order.setArea(new BigDecimal("249.00"));
        order.setCostPerSquareFoot(new BigDecimal("3.50"));
        order.setLaborCostPerSquareFoot(new BigDecimal("4.15"));
        order.setTaxRate(new BigDecimal("25.00"));

        service.calculateOrderCosts(order);

        assertEquals(new BigDecimal("871.50"), order.getMaterialCost());
        assertEquals(new BigDecimal("1033.35"), order.getLaborCost());
        assertEquals(new BigDecimal("476.21"), order.getTax());
        assertEquals(new BigDecimal("2381.06"), order.getTotal());
    }

    // ── getOrder ──────────────────────────────────────────────────────────────

    @Test
    void testGetOrder_exists_returnsOrder() throws Exception {
        Order order = buildOrder(1, FUTURE_DATE);
        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(List.of(order));

        Order result = service.getOrder(FUTURE_DATE, 1);
        assertNotNull(result);
        assertEquals(1, result.getOrderNumber());
    }

    @Test
    void testGetOrder_notFound_throwsNotFoundException() throws Exception {
        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(Collections.emptyList());

        assertThrows(FlooringMasteryOrderNotFoundException.class,
                () -> service.getOrder(FUTURE_DATE, 99));
    }

    // ── removeOrder ───────────────────────────────────────────────────────────

    @Test
    void testRemoveOrder_exists_callsDao() throws Exception {
        Order order = buildOrder(1, FUTURE_DATE);
        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(new ArrayList<>(List.of(order)));

        service.removeOrder(FUTURE_DATE, 1);

        verify(orderDao).removeOrder(FUTURE_DATE, 1);
    }

    @Test
    void testRemoveOrder_notFound_throwsNotFoundException() throws Exception {
        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(Collections.emptyList());

        assertThrows(FlooringMasteryOrderNotFoundException.class,
                () -> service.removeOrder(FUTURE_DATE, 99));
    }

    // ── getNextOrderNumber ────────────────────────────────────────────────────

    @Test
    void testGetNextOrderNumber_noOrders_returnsOne() throws Exception {
        when(orderDao.getAllOrders()).thenReturn(new TreeMap<>());
        assertEquals(1, service.getNextOrderNumber());
    }

    @Test
    void testGetNextOrderNumber_existingOrders_returnsMaxPlusOne() throws Exception {
        Order o1 = buildOrder(3, FUTURE_DATE);
        Order o2 = buildOrder(7, FUTURE_DATE);
        Map<LocalDate, List<Order>> map = new TreeMap<>();
        map.put(FUTURE_DATE, List.of(o1, o2));
        when(orderDao.getAllOrders()).thenReturn(map);

        assertEquals(8, service.getNextOrderNumber());
    }

    // ── exportAllData ─────────────────────────────────────────────────────────

    @Test
    void testExportAllData_callsDaoExport() throws Exception {
        service.exportAllData();
        verify(orderDao).exportAllData("Backup/DataExport.txt");
    }

    // ── editOrder (additional coverage) ──────────────────────────────────────

    @Test
    void testEditOrder_validEdit_savedAndReturned() throws Exception {
        Order existing = buildOrder(1, FUTURE_DATE);
        Order updated  = buildOrder(1, FUTURE_DATE);
        updated.setCustomerName("Updated Name");

        Tax tax       = new Tax("CA", "California", new BigDecimal("25.00"));
        Product product = new Product("Tile", new BigDecimal("3.50"), new BigDecimal("4.15"));

        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(new ArrayList<>(List.of(existing)));
        when(taxDao.getTaxByState("CA")).thenReturn(tax);
        when(productDao.getProductByType("Tile")).thenReturn(product);
        when(orderDao.editOrder(eq(FUTURE_DATE), any(Order.class))).thenAnswer(i -> i.getArgument(1));

        Order result = service.editOrder(FUTURE_DATE, updated);

        assertNotNull(result);
        assertEquals("Updated Name", result.getCustomerName());
        verify(orderDao).editOrder(eq(FUTURE_DATE), any(Order.class));
    }

    @Test
    void testEditOrder_orderNotFound_throwsNotFoundException() throws Exception {
        Order updated = buildOrder(99, FUTURE_DATE);
        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(Collections.emptyList());

        assertThrows(FlooringMasteryOrderNotFoundException.class,
                () -> service.editOrder(FUTURE_DATE, updated));
    }

    @Test
    void testEditOrder_invalidState_throwsValidationException() throws Exception {
        Order existing = buildOrder(1, FUTURE_DATE);
        Order updated  = buildOrder(1, FUTURE_DATE);
        updated.setState("ZZ");

        when(orderDao.getOrdersByDate(FUTURE_DATE)).thenReturn(new ArrayList<>(List.of(existing)));
        when(taxDao.getTaxByState("ZZ")).thenReturn(null);

        assertThrows(FlooringMasteryDataValidationException.class,
                () -> service.editOrder(FUTURE_DATE, updated));
    }

    // ── delegation tests ──────────────────────────────────────────────────────

    @Test
    void testGetAllProducts_delegatesToDao() throws Exception {
        Product p = new Product("Tile", new BigDecimal("3.50"), new BigDecimal("4.15"));
        when(productDao.getAllProducts()).thenReturn(List.of(p));

        List<Product> result = service.getAllProducts();

        assertEquals(1, result.size());
        verify(productDao).getAllProducts();
    }

    @Test
    void testGetTaxByState_delegatesToDao() throws Exception {
        Tax tax = new Tax("CA", "California", new BigDecimal("25.00"));
        when(taxDao.getTaxByState("CA")).thenReturn(tax);

        Tax result = service.getTaxByState("CA");

        assertNotNull(result);
        assertEquals("CA", result.getStateAbbreviation());
        verify(taxDao).getTaxByState("CA");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Order buildOrder(int number, LocalDate date) {
        Order o = new Order(number, date);
        o.setCustomerName("Ada Lovelace");
        o.setState("CA");
        o.setTaxRate(new BigDecimal("25.00"));
        o.setProductType("Tile");
        o.setArea(new BigDecimal("249.00"));
        o.setCostPerSquareFoot(new BigDecimal("3.50"));
        o.setLaborCostPerSquareFoot(new BigDecimal("4.15"));
        o.setMaterialCost(new BigDecimal("871.50"));
        o.setLaborCost(new BigDecimal("1033.35"));
        o.setTax(new BigDecimal("476.21"));
        o.setTotal(new BigDecimal("2381.06"));
        return o;
    }
}
