package com.hideruu.tofutrack1;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {
    private static final List<CartItem> cartItems = new ArrayList<>();

    // Method to add or update item in the cart
    public static void addItemToCart(DataClass product, int quantity) {
        // Check if the item already exists in the cart by prodName
        for (CartItem item : cartItems) {
            if (item.getProduct().getProdName().equals(product.getProdName())) {
                // Item exists, update the quantity and total price
                int newQuantity = item.getQuantity() + quantity;
                item.setQuantity(newQuantity); // Update quantity
                item.setTotalPrice(newQuantity * product.getProdCost()); // Update total price
                return;
            }
        }
        // Item does not exist, add it to the cart
        CartItem newItem = new CartItem(product, quantity, quantity * product.getProdCost());
        cartItems.add(newItem);
    }

    public static List<CartItem> getCartItems() {
        return cartItems;
    }

    public static void clearCart() {
        cartItems.clear();
    }

    public static int getItemCount() {
        int totalItems = 0;
        for (CartItem item : cartItems) {
            totalItems += item.getQuantity();
        }
        return totalItems;
    }

    public static double getTotalPrice() {
        double totalPrice = 0;
        for (CartItem item : cartItems) {
            totalPrice += item.getTotalPrice(); // Sum of all total prices
        }
        return totalPrice;
    }
}
