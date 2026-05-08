package com.sg.flooringmastery.model;

import java.math.BigDecimal;

/**
 * Model of a state's tax information.
 * The tax rate is stored as a whole-number percentage (e.g. 25.00 means 25%).
 */
public class Tax {

    private String stateAbbreviation;
    private String stateName;
    private BigDecimal taxRate;

    public Tax() {}

    public Tax(String stateAbbreviation, String stateName, BigDecimal taxRate) {
        this.stateAbbreviation = stateAbbreviation;
        this.stateName = stateName;
        this.taxRate = taxRate;
    }

    public String getStateAbbreviation() { return stateAbbreviation; }
    public void setStateAbbreviation(String stateAbbreviation) {
        this.stateAbbreviation = stateAbbreviation;
    }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
}
