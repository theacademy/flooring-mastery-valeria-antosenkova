package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductDaoFileImplTest {

    private ProductDao dao;

    @BeforeEach
    void setUp() {
        dao = new ProductDaoFileImpl("TestData/Products.txt");
    }

    @Test
    void testGetAllProducts_returnsExpectedCount() throws Exception {
        List<Product> products = dao.getAllProducts();
        assertNotNull(products);
        assertEquals(4, products.size());
    }

    @Test
    void testGetProductByType_knownProduct_returnsCorrect() throws Exception {
        Product p = dao.getProductByType("Tile");
        assertNotNull(p);
        assertEquals("Tile", p.getProductType());
        assertEquals(new BigDecimal("3.50"), p.getCostPerSquareFoot());
        assertEquals(new BigDecimal("4.15"), p.getLaborCostPerSquareFoot());
    }

    @Test
    void testGetProductByType_carpet() throws Exception {
        Product p = dao.getProductByType("Carpet");
        assertNotNull(p);
        assertEquals(new BigDecimal("2.25"), p.getCostPerSquareFoot());
        assertEquals(new BigDecimal("2.10"), p.getLaborCostPerSquareFoot());
    }

    @Test
    void testGetProductByType_unknownProduct_returnsNull() throws Exception {
        Product p = dao.getProductByType("Granite");
        assertNull(p);
    }

    @Test
    void testGetAllProducts_containsExpectedTypes() throws Exception {
        List<Product> products = dao.getAllProducts();
        long count = products.stream()
                .filter(p -> p.getProductType().equals("Wood"))
                .count();
        assertEquals(1, count);
    }
}
