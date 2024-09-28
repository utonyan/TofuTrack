package com.hideruu.tofutrack1;

public class UpdateRecord {

    private String prodName;
    private int prodQty;
    private double prodCost;
    private String prodUnitType;
    private String prodGroup;
    private String timestamp;

    public UpdateRecord() {
        // Default constructor required for calls to DataSnapshot.getValue(UpdateRecord.class)
    }

    public UpdateRecord(String prodName, int prodQty, double prodCost, String prodUnitType, String prodGroup, String timestamp) {
        this.prodName = prodName;
        this.prodQty = prodQty;
        this.prodCost = prodCost;
        this.prodUnitType = prodUnitType;
        this.prodGroup = prodGroup;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getProdName() {
        return prodName;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public int getProdQty() {
        return prodQty;
    }

    public void setProdQty(int prodQty) {
        this.prodQty = prodQty;
    }

    public double getProdCost() {
        return prodCost;
    }

    public void setProdCost(double prodCost) {
        this.prodCost = prodCost;
    }

    public String getProdUnitType() {
        return prodUnitType;
    }

    public void setProdUnitType(String prodUnitType) {
        this.prodUnitType = prodUnitType;
    }

    public String getProdGroup() {
        return prodGroup;
    }

    public void setProdGroup(String prodGroup) {
        this.prodGroup = prodGroup;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
