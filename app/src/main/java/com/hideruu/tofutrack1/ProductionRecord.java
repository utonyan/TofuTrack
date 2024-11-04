package com.hideruu.tofutrack1;

import java.util.Date;
import java.util.Map;

public class ProductionRecord {
    private String productName;
    private int quantityProduced;
    private double totalPrice;
    private Date timestamp;
    private Map<String, Integer> rawMaterials; // Map of raw material names and their quantities
    private Map<String, Integer> packaging; // Map of packaging names and their quantities

    public ProductionRecord() {
        // Firestore requires a public no-argument constructor
    }

    public ProductionRecord(String productName, int quantityProduced, double totalPrice,
                            Map<String, Integer> rawMaterials, Map<String, Integer> packaging) {
        this.productName = productName;
        this.quantityProduced = quantityProduced;
        this.totalPrice = totalPrice;
        this.timestamp = new Date(); // Set current date
        this.rawMaterials = rawMaterials; // Initialize raw materials
        this.packaging = packaging; // Initialize packaging
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

    public Map<String, Integer> getRawMaterials() {
        return rawMaterials;
    }

    public void setRawMaterials(Map<String, Integer> rawMaterials) {
        this.rawMaterials = rawMaterials;
    }

    public Map<String, Integer> getPackaging() {
        return packaging;
    }

    public void setPackaging(Map<String, Integer> packaging) {
        this.packaging = packaging;
    }
}
