package com.sg.flooringmastery.dao;

import com.sg.flooringmastery.model.Tax;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaxDaoFileImplTest {

    private TaxDao dao;

    @BeforeEach
    void setUp() {
        dao = new TaxDaoFileImpl("TestData/Taxes.txt");
    }

    @Test
    void testGetAllTaxes_returnsExpectedCount() throws Exception {
        List<Tax> taxes = dao.getAllTaxes();
        assertNotNull(taxes);
        assertEquals(4, taxes.size());
    }

    @Test
    void testGetTaxByState_knownState_returnsCorrectTax() throws Exception {
        Tax tax = dao.getTaxByState("CA");
        assertNotNull(tax);
        assertEquals("CA", tax.getStateAbbreviation());
        assertEquals("California", tax.getStateName());
        assertEquals(new BigDecimal("25.00"), tax.getTaxRate());
    }

    @Test
    void testGetTaxByState_caseInsensitive() throws Exception {
        Tax tax = dao.getTaxByState("ca");
        assertNotNull(tax);
        assertEquals("CA", tax.getStateAbbreviation());
    }

    @Test
    void testGetTaxByState_unknownState_returnsNull() throws Exception {
        Tax tax = dao.getTaxByState("ZZ");
        assertNull(tax);
    }

    @Test
    void testGetTaxByState_TX() throws Exception {
        Tax tax = dao.getTaxByState("TX");
        assertNotNull(tax);
        assertEquals(new BigDecimal("4.45"), tax.getTaxRate());
    }
}
