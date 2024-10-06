package com.hideruu.tofutrack1;

import java.util.HashMap;
import java.util.Map;

public class ShoppingCart {
    private Map<String, CartItem> cartItems; // Changed to use prodName as key

    public ShoppingCart() {
        cartItems = new HashMap<>();
    }

    public void addItem(DataClass product, int quantity) {
        String productName = product.getProdName(); // Use prodName as the key
        if (cartItems.containsKey(productName)) {
            CartItem existingItem = cartItems.get(productName);
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            cartItems.put(productName, new CartItem(product, quantity));
        }
    }

    public Map<String, CartItem> getCartItems() {
        return cartItems;
    }

    public double calculateTotalCost() {
        double totalCost = 0;
        for (CartItem item : cartItems.values()) {
            totalCost += item.getProduct().getProdCost() * item.getQuantity();
        }
        return totalCost;
    }

    public static class CartItem {
        private DataClass product;
        private int quantity;

        public CartItem(DataClass product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public DataClass getProduct() {
            return product;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}