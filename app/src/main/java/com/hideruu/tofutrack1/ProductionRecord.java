package com.hideruu.tofutrack1;

import java.util.Date;

public class ProductionRecord {
    private String productName;
    private int quantityProduced;
    private double totalPrice;
    private Date timestamp;

    public ProductionRecord() {
        // Firestore requires a public no-argument constructor
    }

    public ProductionRecord(String productName, int quantityProduced, double totalPrice) {
        this.productName = productName;
        this.quantityProduced = quantityProduced;
        this.totalPrice = totalPrice;
        this.timestamp = new Date(); // Set current date
    }

    // Getters and setters
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantityProduced() {
        return quantityProduced;
    }

    public void setQuantityProduced(int quantityProduced) {
        this.quantityProduced = quantityProduced;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
