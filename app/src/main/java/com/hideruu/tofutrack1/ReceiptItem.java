package com.hideruu.tofutrack1;

public class ReceiptItem {
    private String prodName;
    private double prodCost;
    private String prodUnit;
    private int quantity;

    // No-argument constructor required by Firestore
    public ReceiptItem() {}

    public ReceiptItem(String prodName, double prodCost, String prodUnit, int quantity) {
        this.prodName = prodName;
        this.prodCost = prodCost;
        this.prodUnit = prodUnit;
        this.quantity = quantity;
    }

    public String getProdName() {
        return prodName;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public double getProdCost() {
        return prodCost;
    }

    public void setProdCost(double prodCost) {
        this.prodCost = prodCost;
    }

    public String getProdUnit() {
        return prodUnit;
    }

    public void setProdUnit(String prodUnit) {
        this.prodUnit = prodUnit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
