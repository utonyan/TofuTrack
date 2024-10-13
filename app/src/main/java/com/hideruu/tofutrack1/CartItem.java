package com.hideruu.tofutrack1;

public class CartItem {
    private DataClass product; // Assuming this is your product class
    private int quantity;
    private double totalPrice;

    public CartItem(DataClass product, int quantity, double totalPrice) {
        this.product = product;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public DataClass getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalPrice = quantity * product.getProdCost(); // Update total price when quantity changes
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
