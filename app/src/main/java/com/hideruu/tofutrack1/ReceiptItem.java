package com.hideruu.tofutrack1;

public class ReceiptItem {
    private String prodName;
    private double prodCost;
    private String prodUnit;
    private int quantity;

    public ReceiptItem(String prodName, double prodCost, String prodUnit, int quantity) {
        this.prodName = prodName;
        this.prodCost = prodCost;
        this.prodUnit = prodUnit;
        this.quantity = quantity;
    }

    public String getProdName() {
        return prodName;
    }

    public double getProdCost() {
        return prodCost;
    }

    public String getProdUnit() {
        return prodUnit;
    }

    public int getQuantity() {
        return quantity;
    }
}
