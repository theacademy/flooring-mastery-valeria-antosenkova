package com.sg.flooringmastery.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single flooring order.
 * The calculated fields (materialCost, laborCost, tax, total) are filled in
 * by the service layer after the user enters the area and picks a product.
 */
public class Order {

    private int orderNumber;
    private String customerName;
    private String state;
    private BigDecimal taxRate;
    private String productType;
    private BigDecimal area;
    private BigDecimal costPerSquareFoot;
    private BigDecimal laborCostPerSquareFoot;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal tax;
    private BigDecimal total;
    private LocalDate orderDate;

    public Order() {}

    public Order(int orderNumber, LocalDate orderDate) {
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
    }

    public int getOrderNumber() { return orderNumber; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }

    public BigDecimal getCostPerSquareFoot() { return costPerSquareFoot; }
    public void setCostPerSquareFoot(BigDecimal costPerSquareFoot) {
        this.costPerSquareFoot = costPerSquareFoot;
    }

    public BigDecimal getLaborCostPerSquareFoot() { return laborCostPerSquareFoot; }
    public void setLaborCostPerSquareFoot(BigDecimal laborCostPerSquareFoot) {
        this.laborCostPerSquareFoot = laborCostPerSquareFoot;
    }

    public BigDecimal getMaterialCost() { return materialCost; }
    public void setMaterialCost(BigDecimal materialCost) { this.materialCost = materialCost; }

    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    @Override
    public String toString() {
        return String.format("Order #%d | %s | %s | %s | Area: %s sqft | Total: $%s",
                orderNumber, customerName, state, productType, area, total);
    }
}
